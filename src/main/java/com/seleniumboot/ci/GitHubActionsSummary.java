package com.seleniumboot.ci;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.reporting.ReportPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Appends a Markdown summary table of failed tests with their error message and trace links
 * to GitHub Actions' {@code $GITHUB_STEP_SUMMARY}.
 *
 * <p>Activated when running in GitHub Actions, {@code tracing.enabled: true} is configured,
 * and at least one test failed. Outside GitHub Actions or when no tests failed, this is a no-op.
 */
public final class GitHubActionsSummary {

    private GitHubActionsSummary() {}

    /**
     * Builds the Markdown table string for any failed tests in {@code timings}.
     * Returns an empty string if {@code timings} is null or contains no failed tests.
     */
    public static String buildMarkdown(Collection<TestTiming> timings) {
        return buildMarkdown(timings, null);
    }

    /**
     * Same as {@link #buildMarkdown(Collection)}, and when {@code runUrl} is set, adds a line
     * under the table linking to the workflow run page where uploaded trace artifacts live.
     */
    public static String buildMarkdown(Collection<TestTiming> timings, String runUrl) {
        if (timings == null || timings.isEmpty()) {
            return "";
        }

        List<TestTiming> failed = timings.stream()
                .filter(t -> "FAILED".equalsIgnoreCase(t.getStatus()))
                .toList();

        if (failed.isEmpty()) {
            return "";
        }

        String baseDir = ReportPaths.baseDir();

        StringBuilder sb = new StringBuilder();
        sb.append("### Failed Tests\n\n");
        sb.append("| Test | Error | Trace |\n");
        sb.append("| --- | --- | --- |\n");

        for (TestTiming t : failed) {
            String testId = escapeTableCell(t.getTestId() != null ? t.getTestId() : "Unknown");
            String error = escapeTableCell(t.getErrorMessage() != null ? t.getErrorMessage() : "—");
            String tracePath = resolveTracePath(t, baseDir);

            sb.append("| ").append(testId)
              .append(" | ").append(error)
              .append(" | ").append(tracePath)
              .append(" |\n");
        }

        if (runUrl != null && !runUrl.isBlank()) {
            sb.append("\nTraces are in this run's [artifacts](").append(runUrl.trim())
              .append(") if `").append(base(baseDir)).append("/traces/` is uploaded with `actions/upload-artifact`.\n");
        }

        return sb.toString();
    }

    /**
     * Builds the workflow run page URL from the GitHub Actions environment, or {@code null}
     * when any of the three values is missing.
     */
    public static String runUrl(String serverUrl, String repository, String runId) {
        if (isBlank(serverUrl) || isBlank(repository) || isBlank(runId)) {
            return null;
        }
        String server = serverUrl.trim();
        if (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        return server + "/" + repository.trim() + "/actions/runs/" + runId.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String base(String baseDir) {
        return isBlank(baseDir) ? "target" : baseDir.replace('\\', '/').trim().replaceAll("/+$", "");
    }

    /**
     * Appends failed tests with trace links to {@code summaryFile} when tracing is enabled
     * and at least one test failed.
     *
     * @return {@code true} if summary content was appended; {@code false} otherwise.
     */
    public static boolean appendSummary(SeleniumBootConfig config,
                                        Collection<TestTiming> timings,
                                        File summaryFile) {
        if (config == null) {
            return false;
        }
        SeleniumBootConfig.Tracing tracing = config.getTracing();
        if (tracing == null || !tracing.isEffectivelyEnabled()) {
            return false;
        }
        if (timings == null || timings.isEmpty() || summaryFile == null) {
            return false;
        }

        boolean hasFailures = timings.stream()
                .anyMatch(t -> "FAILED".equalsIgnoreCase(t.getStatus()));
        if (!hasFailures) {
            return false;
        }

        String markdown = buildMarkdown(timings,
                runUrl(System.getenv("GITHUB_SERVER_URL"), System.getenv("GITHUB_REPOSITORY"), System.getenv("GITHUB_RUN_ID")));
        if (markdown.isEmpty()) {
            return false;
        }

        try {
            File parent = summaryFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            boolean prependNewline = summaryFile.exists() && summaryFile.length() > 0;
            String content = (prependNewline ? "\n" : "") + markdown;

            try (FileWriter writer = new FileWriter(summaryFile, StandardCharsets.UTF_8, true)) {
                writer.write(content);
            }

            System.out.println("[Selenium Boot] Appended failed tests to GitHub Actions summary → "
                    + summaryFile.getPath());
            return true;
        } catch (IOException e) {
            System.err.println("[Selenium Boot] Failed to write to $GITHUB_STEP_SUMMARY: "
                    + e.getMessage());
            return false;
        }
    }

    /**
     * Exports failed tests to {@code $GITHUB_STEP_SUMMARY} if running in GitHub Actions.
     *
     * @return {@code true} if written; {@code false} if skipped or failed.
     */
    public static boolean export(SeleniumBootConfig config, Collection<TestTiming> timings) {
        String testOverride = System.getProperty("seleniumboot.github.step.summary");
        boolean isActions = CiEnvironmentDetector.isGitHubActions()
                || (testOverride != null && !testOverride.isBlank());
        if (!isActions) {
            return false;
        }

        String summaryPath = (testOverride != null && !testOverride.isBlank())
                ? testOverride.trim()
                : System.getenv("GITHUB_STEP_SUMMARY");
        if (summaryPath == null || summaryPath.isBlank()) {
            return false;
        }

        return appendSummary(config, timings, new File(summaryPath));
    }

    /**
     * Overload using the globally configured {@link SeleniumBootConfig}.
     */
    public static boolean export(Collection<TestTiming> timings) {
        SeleniumBootConfig config = null;
        try {
            config = SeleniumBootContext.getConfig();
        } catch (Exception ignored) {}
        return export(config, timings);
    }

    static String resolveTracePath(TestTiming timing, String baseDir) {
        String base = (baseDir != null && !baseDir.isBlank())
                ? baseDir.replace('\\', '/').trim()
                : "target";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = timing.getTracePath();
        if (path != null && !path.isBlank()) {
            String normalized = path.replace('\\', '/').trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.startsWith(base + "/")) {
                return normalized;
            }
            return base + "/" + normalized;
        }

        String className = timing.getTestClassName() != null && !timing.getTestClassName().isBlank()
                ? timing.getTestClassName()
                : "UnknownClass";
        String testId = timing.getTestId();
        String methodName = extractMethodName(testId);
        return base + "/traces/" + className + "/" + sanitize(methodName) + "-trace.html";
    }

    static String extractMethodName(String testId) {
        if (testId == null || testId.isBlank()) {
            return "unknown";
        }
        int hashIdx = testId.indexOf('#');
        if (hashIdx >= 0 && hashIdx < testId.length() - 1) {
            return testId.substring(hashIdx + 1);
        }
        int dotIdx = testId.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < testId.length() - 1) {
            return testId.substring(dotIdx + 1);
        }
        return testId;
    }

    static String escapeTableCell(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.replace("\r\n", " ")
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replace("|", "\\|")
                    .trim();
    }

    private static String sanitize(String s) {
        return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}

