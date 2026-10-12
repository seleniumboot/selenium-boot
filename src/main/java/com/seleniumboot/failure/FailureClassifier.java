package com.seleniumboot.failure;

import com.seleniumboot.metrics.TestTiming;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tags a failed test with a {@link FailureCategory} and the evidence for it, using only the
 * error message and stack trace the framework already records. No AI, no network.
 *
 * <p>The exception class chain is read from the printed stack trace (the outer throwable on the
 * first line, each cause on a {@code Caused by:} line), so a {@code LocatorException} whose cause
 * is a {@code TimeoutException} is a locator problem, while a bare {@code TimeoutException} is not.
 * Anything the rules do not recognise is {@link FailureCategory#UNKNOWN}; the classifier never guesses.
 */
public final class FailureClassifier {

    /** Category plus the one-line reason it was chosen. */
    public record Result(FailureCategory category, String evidence) { }

    private static final Pattern THROWABLE_LINE = Pattern.compile(
            "^(?:Caused by: )?((?:[A-Za-z_$][\\w$]*\\.)+[A-Za-z_$][\\w$]*|[A-Za-z_$][\\w$]*(?:Exception|Error))"
            + "(?::\\s?(.*))?$");

    private static final int EVIDENCE_MAX = 140;

    private FailureClassifier() { }

    public static Result classify(TestTiming timing) {
        if (timing == null) return unknown("no failure data");
        return classify(timing.getErrorMessage(), timing.getStackTrace());
    }

    public static Result classify(String errorMessage, String stackTrace) {
        List<String> chain = causeChain(stackTrace);
        String outer = chain.isEmpty() ? null : chain.get(0);
        String haystack = ((errorMessage != null ? errorMessage : "") + "\n"
                + (stackTrace != null ? stackTrace : "")).toLowerCase(Locale.ROOT);

        if (chain.isEmpty() && (errorMessage == null || errorMessage.isBlank())) {
            return unknown("no error message or stack trace recorded");
        }

        // A failed assertion is the test doing its job, whatever text its message happens to contain.
        if (outer != null && isAssertion(outer)) {
            return new Result(FailureCategory.ASSERTION_MISMATCH, "assertion failed (" + simple(outer) + ")");
        }

        String hit = firstMatch(chain, "NoSuchSessionException", "UnreachableBrowserException",
                "SessionNotCreatedException");
        if (hit != null) return result(FailureCategory.BROWSER_CRASH, simple(hit) + " in cause chain");
        String crash = containsAny(haystack, "invalid session id", "session deleted because of page crash",
                "chrome not reachable", "not connected to devtools", "target frame detached",
                "browser has closed the connection", "tab crashed");
        if (crash != null) return result(FailureCategory.BROWSER_CRASH, "message contains \"" + crash + "\"");

        String nav = containsAny(haystack, "net::err_", "err_name_not_resolved", "dnsnotfound",
                "reached error page", "unknownhostexception", "timed out receiving message from renderer",
                "page load timeout", "pageloadstrategy");
        if (nav != null) return result(FailureCategory.NAVIGATION_ERROR, "message contains \"" + nav + "\"");
        Matcher http = Pattern.compile("(?:http|status)[ :=]+(5\\d\\d)\\b").matcher(haystack);
        if (http.find()) return result(FailureCategory.NAVIGATION_ERROR, "HTTP " + http.group(1) + " in message");

        hit = firstMatch(chain, "StaleElementReferenceException");
        if (hit != null) return result(FailureCategory.STALE_ELEMENT, simple(hit) + " in cause chain");

        hit = firstMatch(chain, "ElementNotInteractableException", "ElementClickInterceptedException",
                "InvalidElementStateException", "ElementNotSelectableException");
        if (hit != null) return result(FailureCategory.ELEMENT_NOT_INTERACTABLE, simple(hit) + " in cause chain");

        hit = firstMatch(chain, "LocatorException", "NoSuchElementException", "NoSuchFrameException",
                "NoSuchShadowRootException");
        if (hit != null) return result(FailureCategory.LOCATOR_NOT_FOUND, simple(hit) + " in cause chain");

        hit = firstMatch(chain, "TimeoutException");
        if (hit != null) return result(FailureCategory.TIMEOUT, simple(hit) + " with no locator or navigation cause");

        for (String c : chain) {
            if (isAssertion(c)) {
                return result(FailureCategory.ASSERTION_MISMATCH, simple(c) + " in cause chain");
            }
        }
        if (haystack.contains("the following asserts failed")) {
            return result(FailureCategory.ASSERTION_MISMATCH, "soft assertions failed");
        }

        return unknown(outer != null ? "unrecognised " + simple(outer) : "unrecognised error");
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

    private static Result result(FailureCategory c, String evidence) {
        return new Result(c, evidence.length() > EVIDENCE_MAX ? evidence.substring(0, EVIDENCE_MAX) + "…" : evidence);
    }

    private static Result unknown(String evidence) {
        return new Result(FailureCategory.UNKNOWN, evidence);
    }
}
