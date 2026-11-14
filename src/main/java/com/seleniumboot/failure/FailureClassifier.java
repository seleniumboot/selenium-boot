package com.seleniumboot.failure;

import com.seleniumboot.flakiness.TestStatus;
import com.seleniumboot.metrics.TestTiming;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tags a failed test with a {@link FailureCategory}, a high-level {@link FailureType}, the
 * evidence for it, and actionable remediation advice, using only the error message and stack trace
 * the framework already records. No AI, no network.
 *
 * <p>The exception class chain is read from the printed stack trace (the outer throwable on the
 * first line, each cause on a {@code Caused by:} line), so a {@code LocatorException} whose cause
 * is a {@code TimeoutException} is a locator problem, while a bare {@code TimeoutException} is not.
 * Anything the rules do not recognise is {@link FailureCategory#UNKNOWN}; the classifier never guesses.
 */
public final class FailureClassifier {

    /** Category, high-level triage type, the one-line reason, and actionable remediation advice. */
    public record Result(FailureCategory category, FailureType type, String evidence, String suggestedAction) {
        public Result(FailureCategory category, String evidence) {
            this(category, category != null ? category.defaultType() : FailureType.UNKNOWN,
                    evidence, category != null ? category.defaultAction() : "");
        }

        public Result(FailureCategory category, FailureType type, String evidence) {
            this(category, type, evidence, category != null ? category.defaultAction() : "");
        }

        /**
         * Human-readable label for triage. When flaky, includes the underlying failure cause
         * (e.g. "Flaky (Locator not found)").
         */
        public String displayLabel() {
            if (type == FailureType.FLAKY && category != null && category != FailureCategory.UNKNOWN) {
                return "Flaky (" + category.label() + ")";
            }
            return type != null ? type.label() : "Unknown";
        }
    }

    // Possessive quantifiers: a long dotted line must not recurse once per segment (StackOverflowError).
    private static final Pattern THROWABLE_LINE = Pattern.compile(
            "^(?:Caused by: )?((?:[A-Za-z_$][\\w$]*+\\.)++[A-Za-z_$][\\w$]*+|[A-Za-z_$][\\w$]*(?:Exception|Error))"
            + "(?::\\s?(.*))?$");

    private static final int EVIDENCE_MAX = 140;

    private record CategoryMatch(FailureCategory category, String evidence) { }

    private FailureClassifier() { }

    public static Result classify(TestTiming timing) {
        return classify(timing, null);
    }

    public static Result classify(TestTiming timing, TestStatus status) {
        if (timing == null) return unknown("no failure data");
        Result base = classify(timing.getErrorMessage(), timing.getStackTrace(), status);
        if (base.category() == FailureCategory.LOCATOR_NOT_FOUND) {
            String locator = timing.getFailingLocator();
            if (locator != null && !locator.isBlank()) {
                String enrichedAction = "Check if locator '" + locator
                        + "' has changed or if the element is rendered in an iframe/shadow DOM or delayed by async loading.";
                return new Result(base.category(), base.type(), base.evidence(), enrichedAction);
            }
        }
        return base;
    }

    public static Result classify(String errorMessage, String stackTrace) {
        return classify(errorMessage, stackTrace, null);
    }

    public static Result classify(String errorMessage, String stackTrace, TestStatus status) {
        List<String> chain = causeChain(stackTrace);
        String outer = chain.isEmpty() ? null : chain.get(0);
        String haystack = ((errorMessage != null ? errorMessage : "") + "\n"
                + (stackTrace != null ? stackTrace : "")).toLowerCase(Locale.ROOT);

        if (chain.isEmpty() && (errorMessage == null || errorMessage.isBlank())) {
            return unknown("no error message or stack trace recorded");
        }

        CategoryMatch match = matchCategory(chain, outer, haystack);
        if (match == null) {
            return unknown(outer != null ? "unrecognised " + simple(outer) : "unrecognised error");
        }

        FailureCategory category = match.category();
        String evidence = match.evidence();
        FailureType type = category.defaultType();
        String action = category.defaultAction();

        if (status == TestStatus.PASSED_ON_RETRY || status == TestStatus.INTERMITTENT) {
            type = FailureType.FLAKY;
            action = "Test passed on retry (" + status.label().toLowerCase(Locale.ROOT)
                    + ") with intermittent " + category.label()
                    + ". Investigate timing, dynamic DOM delays, or transient conditions.";
        } else if (status == TestStatus.NEWLY_FAILING && category == FailureCategory.ASSERTION_MISMATCH) {
            type = FailureType.PRODUCT_BUG;
            action = "Newly failing test with assertion mismatch. Verify whether the application UI regressed in the latest build, or if requirements/mock data changed and the test expectation needs updating.";
        } else if (category == FailureCategory.NAVIGATION_ERROR && evidence != null && evidence.startsWith("HTTP 5")) {
            action = "Server returned an HTTP 5xx error. Inspect backend service logs and verify application server health.";
        }

        return result(category, type, evidence, action);
    }

    private static CategoryMatch matchCategory(List<String> chain, String outer, String haystack) {
        // 1. A failed assertion on the outer throwable is the test doing its job.
        if (outer != null && isAssertion(outer)) {
            return new CategoryMatch(FailureCategory.ASSERTION_MISMATCH, "assertion failed (" + simple(outer) + ")");
        }

        // 2. Fatal browser crash exceptions in the cause chain
        String hit = firstMatch(chain, "NoSuchSessionException", "UnreachableBrowserException",
                "SessionNotCreatedException");
        if (hit != null) {
            return new CategoryMatch(FailureCategory.BROWSER_CRASH, simple(hit) + " in cause chain");
        }

        // 3. Element exceptions in cause chain take precedence over error message text
        // (e.g. NoSuchElementException must not be matched as Navigation error from capabilities dumps)
        hit = firstMatch(chain, "StaleElementReferenceException");
        if (hit != null) {
            return new CategoryMatch(FailureCategory.STALE_ELEMENT, simple(hit) + " in cause chain");
        }

        hit = firstMatch(chain, "ElementNotInteractableException", "ElementClickInterceptedException",
                "InvalidElementStateException", "ElementNotSelectableException");
        if (hit != null) {
            return new CategoryMatch(FailureCategory.ELEMENT_NOT_INTERACTABLE, simple(hit) + " in cause chain");
        }

        hit = firstMatch(chain, "LocatorException", "NoSuchElementException", "NoSuchFrameException",
                "NoSuchShadowRootException");
        if (hit != null) {
            return new CategoryMatch(FailureCategory.LOCATOR_NOT_FOUND, simple(hit) + " in cause chain");
        }

        // 4. Browser crash message text
        String crash = containsAny(haystack, "invalid session id", "session deleted because of page crash",
                "chrome not reachable", "not connected to devtools", "target frame detached",
                "browser has closed the connection", "tab crashed");
        if (crash != null) {
            return new CategoryMatch(FailureCategory.BROWSER_CRASH, "message contains \"" + crash + "\"");
        }

        // 5. Navigation message text (pageloadstrategy intentionally excluded)
        String nav = containsAny(haystack, "net::err_", "err_name_not_resolved", "dnsnotfound",
                "reached error page", "unknownhostexception", "timed out receiving message from renderer",
                "page load timeout");
        if (nav != null) {
            return new CategoryMatch(FailureCategory.NAVIGATION_ERROR, "message contains \"" + nav + "\"");
        }

        // 6. HTTP 5xx status codes
        Matcher http = Pattern.compile("(?:http|status)[ :=]+(5\\d\\d)\\b").matcher(haystack);
        if (http.find()) {
            return new CategoryMatch(FailureCategory.NAVIGATION_ERROR, "HTTP " + http.group(1) + " in message");
        }

        // 7. TimeoutException with no locator or navigation cause
        hit = firstMatch(chain, "TimeoutException");
        if (hit != null) {
            return new CategoryMatch(FailureCategory.TIMEOUT, simple(hit) + " with no locator or navigation cause");
        }

        // 8. Assertion exceptions deeper in cause chain or soft assertions in message
        for (String c : chain) {
            if (isAssertion(c)) {
                return new CategoryMatch(FailureCategory.ASSERTION_MISMATCH, simple(c) + " in cause chain");
            }
        }
        if (haystack.contains("the following asserts failed")) {
            return new CategoryMatch(FailureCategory.ASSERTION_MISMATCH, "soft assertions failed");
        }

        return null;
    }

    static List<String> causeChainOf(String stackTrace) {
        return causeChain(stackTrace);
    }

    /** Fully-qualified class names of the outer throwable followed by each cause, in order. */
    static List<String> causeChain(String stackTrace) {
        List<String> chain = new ArrayList<>();
        if (stackTrace == null) return chain;
        boolean first = true;
        for (String line : stackTrace.split("\\R")) {
            if (line.isEmpty() || Character.isWhitespace(line.charAt(0))) continue;
            Matcher m = THROWABLE_LINE.matcher(line);
            boolean cause = line.startsWith("Caused by: ");
            if (first) {
                first = false;
                if (m.matches()) chain.add(m.group(1));
                else return chain;
            } else if (cause && m.matches()) {
                chain.add(m.group(1));
            }
        }
        return chain;
    }

    private static boolean isAssertion(String className) {
        String s = simple(className);
        return s.equals("AssertionError") || s.equals("AssertionFailedError")
                || s.equals("ComparisonFailure") || s.equals("DbAssertException")
                || s.equals("MultipleFailuresError");
    }

    private static String firstMatch(List<String> chain, String... simpleNames) {
        for (String c : chain) {
            for (String n : simpleNames) {
                if (simple(c).equals(n)) return c;
            }
        }
        return null;
    }

    private static String containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return n;
        }
        return null;
    }

    private static String simple(String className) {
        int i = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));
        return i >= 0 ? className.substring(i + 1) : className;
    }

    private static Result result(FailureCategory c, FailureType type, String evidence, String action) {
        String boundedEvidence = evidence.length() > EVIDENCE_MAX ? evidence.substring(0, EVIDENCE_MAX) + "…" : evidence;
        return new Result(c, type, boundedEvidence, action);
    }

    private static Result unknown(String evidence) {
        return new Result(FailureCategory.UNKNOWN, FailureType.UNKNOWN, evidence, FailureCategory.UNKNOWN.defaultAction());
    }
}
