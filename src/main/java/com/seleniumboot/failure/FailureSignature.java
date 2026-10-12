package com.seleniumboot.failure;

import com.seleniumboot.metrics.TestTiming;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stable fingerprint of <em>why</em> a test failed, so that many failures with one cause
 * collapse into one group.
 *
 * <p>The signature joins the {@link FailureCategory}, the outer exception type, the first line of
 * the message with volatile parts (UUIDs, hex ids, numbers, quoted values) replaced by
 * placeholders, and the top application stack frame (class and method, no line number).
 * Two tests share a signature when all four match.
 */
public final class FailureSignature {

    private static final Pattern UUID = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern HEX = Pattern.compile("\\b(?:0x)?[0-9a-fA-F]{12,}\\b");
    private static final Pattern QUOTED = Pattern.compile("'[^']*'|\"[^\"]*\"");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern FRAME = Pattern.compile("^\\s+at ([^(\\s]+)\\(");

    private static final List<String> FRAMEWORK_PREFIXES = List.of(
            "java.", "javax.", "jdk.", "sun.", "org.openqa.", "org.testng.", "org.junit.",
            "io.cucumber.", "com.seleniumboot.");

    private static final int MESSAGE_MAX = 160;

    private FailureSignature() { }

    public static String of(TestTiming timing) {
        return timing == null ? of(null, null) : of(timing.getErrorMessage(), timing.getStackTrace());
    }

    public static String of(String errorMessage, String stackTrace) {
        FailureCategory category = FailureClassifier.classify(errorMessage, stackTrace).category();
        List<String> chain = FailureClassifier.causeChainOf(stackTrace);
        String type = chain.isEmpty() ? "?" : chain.get(0);
        return category.name() + " | " + type + " | " + normalise(errorMessage) + " | " + topAppFrame(stackTrace);
    }

    /** First message line, with ids, numbers and quoted values replaced so recurring failures match. */
    static String normalise(String message) {
        if (message == null) return "";
        String first = message.strip();
        int nl = first.indexOf('\n');
        if (nl >= 0) first = first.substring(0, nl);
        first = UUID.matcher(first).replaceAll("<id>");
        first = HEX.matcher(first).replaceAll("<id>");
        first = QUOTED.matcher(first).replaceAll("<v>");
        first = NUMBER.matcher(first).replaceAll("#");
        first = SPACES.matcher(first).replaceAll(" ").strip();
        return first.length() > MESSAGE_MAX ? first.substring(0, MESSAGE_MAX) : first;
    }

    /**
     * Class and method of the first stack frame outside the JDK, Selenium, the test runner and the
     * framework: the page object or test code that was running. Falls back to the first frame.
     */
    static String topAppFrame(String stackTrace) {
        if (stackTrace == null) return "";
        String firstFrame = null;
        for (String line : stackTrace.split("\\R")) {
            if (line.startsWith("Caused by: ")) break;
            Matcher m = FRAME.matcher(line);
            if (!m.find()) continue;
            String frame = m.group(1);
            if (firstFrame == null) firstFrame = frame;
            if (FRAMEWORK_PREFIXES.stream().noneMatch(frame::startsWith)) return frame;
        }
        return firstFrame != null ? firstFrame : "";
    }
}
