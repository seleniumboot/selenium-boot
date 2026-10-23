package com.seleniumboot.unit;

import com.seleniumboot.ci.GitHubActionsSummary;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.metrics.TestTiming;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link GitHubActionsSummary}.
 */
public class GitHubActionsSummaryTest {

    private File tempSummaryFile;

    @BeforeMethod
    public void setUp() throws IOException {
        tempSummaryFile = File.createTempFile("github-step-summary-test", ".md");
        tempSummaryFile.deleteOnExit();
    }

    @AfterMethod
    public void tearDown() {
        if (tempSummaryFile != null && tempSummaryFile.exists()) {
            tempSummaryFile.delete();
        }
    }

    private TestTiming timing(String testId, String className, String status, String error, String tracePath) {
        TestTiming t = new TestTiming(testId, "main");
        t.setTestClassName(className);
        t.setStatus(status);
        t.setErrorMessage(error);
        if (tracePath != null) {
            t.setTracePath(tracePath);
        }
        return t;
    }

    private SeleniumBootConfig configWithTracing(boolean enabled) {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Tracing tracing = new SeleniumBootConfig.Tracing();
        tracing.setEnabled(enabled);
        config.setTracing(tracing);
        return config;
    }

    // ==========================================================
    // Markdown Generation
    // ==========================================================

    @Test
    public void buildMarkdown_nullOrEmpty_returnsEmptyString() {
        assertEquals("", GitHubActionsSummary.buildMarkdown(null));
        assertEquals("", GitHubActionsSummary.buildMarkdown(Collections.emptyList()));
    }

    @Test
    public void buildMarkdown_onlyPassingOrSkipped_returnsEmptyString() {
        List<TestTiming> timings = List.of(
                timing("t1", "LoginTest", "PASSED", null, null),
                timing("t2", "CheckoutTest", "SKIPPED", null, null)
        );
        assertEquals("", GitHubActionsSummary.buildMarkdown(timings));
    }

    @Test
    public void buildMarkdown_singleFailedTest_generatesTable() {
        TestTiming failed = timing(
                "com.example.LoginTest#testInvalidCredentials",
                "LoginTest",
                "FAILED",
                "Expected [Login] but found [404 Not Found]",
                "traces/LoginTest/testInvalidCredentials-trace.html"
        );

        String markdown = GitHubActionsSummary.buildMarkdown(List.of(failed));

        assertTrue(markdown.contains("### Failed Tests"), "Must contain Failed Tests heading");
        assertTrue(markdown.contains("| Test | Error | Trace |"), "Must contain table header row");
        assertTrue(markdown.contains("| --- | --- | --- |"), "Must contain markdown separator row");
        assertTrue(markdown.contains("com.example.LoginTest#testInvalidCredentials"));
        assertTrue(markdown.contains("Expected [Login] but found [404 Not Found]"));
        assertTrue(markdown.contains("target/traces/LoginTest/testInvalidCredentials-trace.html")
                || markdown.contains("build/traces/LoginTest/testInvalidCredentials-trace.html"),
                "Trace path must include base dir and relative trace file path");
    }

    @Test
    public void buildMarkdown_withRunUrl_linksToRunArtifacts() {
        TestTiming failed = timing("t1", "LoginTest", "FAILED", "boom", "traces/LoginTest/t1-trace.html");

        String markdown = GitHubActionsSummary.buildMarkdown(List.of(failed),
                "https://github.com/acme/app/actions/runs/42");

        assertTrue(markdown.contains("[artifacts](https://github.com/acme/app/actions/runs/42)"),
                "Must link to the workflow run page");
        assertTrue(markdown.contains("actions/upload-artifact"), "Must hint how traces get there");
    }

    @Test
    public void buildMarkdown_withoutRunUrl_hasNoArtifactsLine() {
        TestTiming failed = timing("t1", "LoginTest", "FAILED", "boom", "traces/LoginTest/t1-trace.html");

        assertFalse(GitHubActionsSummary.buildMarkdown(List.of(failed)).contains("artifacts"));
        assertFalse(GitHubActionsSummary.buildMarkdown(List.of(failed), " ").contains("artifacts"));
    }

    @Test
    public void runUrl_buildsFromEnvironmentValues() {
        assertEquals(GitHubActionsSummary.runUrl("https://github.com/", "acme/app", "42"),
                "https://github.com/acme/app/actions/runs/42");
        assertNull(GitHubActionsSummary.runUrl(null, "acme/app", "42"));
        assertNull(GitHubActionsSummary.runUrl("https://github.com", "", "42"));
        assertNull(GitHubActionsSummary.runUrl("https://github.com", "acme/app", null));
    }

    @Test
    public void buildMarkdown_multipleFailedTests_generatesMultipleRows() {
        List<TestTiming> timings = List.of(
                timing("t1", "TestA", "PASSED", null, null),
                timing("t2", "TestB", "FAILED", "Error B", "traces/TestB/t2-trace.html"),
                timing("t3", "TestC", "FAILED", "Error C", "traces/TestC/t3-trace.html")
        );

        String markdown = GitHubActionsSummary.buildMarkdown(timings);

        assertTrue(markdown.contains("t2"));
        assertTrue(markdown.contains("Error B"));
        assertTrue(markdown.contains("t3"));
        assertTrue(markdown.contains("Error C"));
        assertFalse(markdown.contains("t1")); // passed test not included
    }

    @Test
    public void buildMarkdown_escapesPipeAndNewlinesInError() {
        TestTiming failed = timing(
                "com.example.PipeTest#testPipe",
                "PipeTest",
                "FAILED",
                "Value a | b failed\r\nnext line",
                "traces/PipeTest/testPipe-trace.html"
        );

        String markdown = GitHubActionsSummary.buildMarkdown(List.of(failed));

        // The error line in markdown table row must escape '|' as '\|'
        assertTrue(markdown.contains("Value a \\| b failed next line"));
        // Check that table row doesn't break into multiple unescaped lines
        String[] lines = markdown.split("\n");
        long rowLines = java.util.Arrays.stream(lines).filter(l -> l.startsWith("| com.example.PipeTest")).count();
        assertEquals(1, rowLines, "Table row must be a single line");
    }

    @Test
    public void buildMarkdown_nullErrorMessage_usesFallbackDash() {
        TestTiming failed = timing(
                "com.example.NullErr#testNull",
                "NullErr",
                "FAILED",
                null,
                "traces/NullErr/testNull-trace.html"
        );

        String markdown = GitHubActionsSummary.buildMarkdown(List.of(failed));

        assertTrue(markdown.contains("| — |"));
    }

    // ==========================================================
    // Trace Path Resolution
    // ==========================================================

    @Test
    public void resolveTracePath_handlesRelativeAndPrefixedPaths() {
        TestTiming withRelative = timing("com.example.ClassA.methodA", "ClassA", "FAILED", "err",
                "traces/ClassA/methodA-trace.html");
        String resolvedRel = GitHubActionsSummary.buildMarkdown(List.of(withRelative));
        assertTrue(resolvedRel.contains("target/traces/ClassA/methodA-trace.html")
                || resolvedRel.contains("build/traces/ClassA/methodA-trace.html"));

        TestTiming alreadyPrefixed = timing("com.example.ClassB.methodB", "ClassB", "FAILED", "err",
                "target/traces/ClassB/methodB-trace.html");
        String resolvedPrefixed = GitHubActionsSummary.buildMarkdown(List.of(alreadyPrefixed));
        assertTrue(resolvedPrefixed.contains("target/traces/ClassB/methodB-trace.html"));
        assertFalse(resolvedPrefixed.contains("target/target/"));
    }

    @Test
    public void resolveTracePath_nullTracePath_generatesDefaultTracePath() {
        TestTiming withoutTrace = timing("com.example.FallbackTest#testFallback", "FallbackTest", "FAILED", "err", null);
        String markdown = GitHubActionsSummary.buildMarkdown(List.of(withoutTrace));
        assertTrue(markdown.contains("traces/FallbackTest/testFallback-trace.html"));
    }

    // ==========================================================
    // appendSummary
    // ==========================================================

    @Test
    public void appendSummary_writesMarkdownToFile() throws IOException {
        SeleniumBootConfig config = configWithTracing(true);
        List<TestTiming> timings = List.of(
                timing("t1", "ClassOne", "FAILED", "Something exploded", "traces/ClassOne/t1-trace.html")
        );

        boolean result = GitHubActionsSummary.appendSummary(config, timings, tempSummaryFile);

        assertTrue(result, "appendSummary should return true on success");
        String content = Files.readString(tempSummaryFile.toPath());
        assertTrue(content.contains("### Failed Tests"));
        assertTrue(content.contains("Something exploded"));
    }

    @Test
    public void appendSummary_appendsToExistingFileContent() throws IOException {
        Files.writeString(tempSummaryFile.toPath(), "## Existing Job Summary Content\n");

        SeleniumBootConfig config = configWithTracing(true);
        List<TestTiming> timings = List.of(
                timing("t1", "ClassOne", "FAILED", "Fail 1", "traces/ClassOne/t1-trace.html")
        );

        boolean result = GitHubActionsSummary.appendSummary(config, timings, tempSummaryFile);

        assertTrue(result);
        String content = Files.readString(tempSummaryFile.toPath());
        assertTrue(content.startsWith("## Existing Job Summary Content\n"));
        assertTrue(content.contains("### Failed Tests"));
    }

    @Test
    public void appendSummary_tracingDisabled_noOp() {
        SeleniumBootConfig config = configWithTracing(false);
        List<TestTiming> timings = List.of(
                timing("t1", "ClassOne", "FAILED", "Fail 1", "traces/ClassOne/t1-trace.html")
        );

        boolean result = GitHubActionsSummary.appendSummary(config, timings, tempSummaryFile);

        assertFalse(result, "appendSummary must no-op when tracing.enabled is false");
        assertEquals(0, tempSummaryFile.length(), "File must not be written to");
    }

    @Test
    public void appendSummary_noFailedTests_noOp() {
        SeleniumBootConfig config = configWithTracing(true);
        List<TestTiming> timings = List.of(
                timing("t1", "ClassOne", "PASSED", null, null)
        );

        boolean result = GitHubActionsSummary.appendSummary(config, timings, tempSummaryFile);

        assertFalse(result, "appendSummary must no-op when nothing failed");
        assertEquals(0, tempSummaryFile.length(), "File must not be written to");
    }

    @Test
    public void appendSummary_createsParentDirectoryIfMissing() {
        File nested = new File(tempSummaryFile.getParentFile(), "nested-dir/summary.md");
        nested.deleteOnExit();
        if (nested.exists()) nested.delete();
        if (nested.getParentFile().exists()) nested.getParentFile().delete();

        SeleniumBootConfig config = configWithTracing(true);
        List<TestTiming> timings = List.of(
                timing("t1", "ClassOne", "FAILED", "Fail 1", "traces/ClassOne/t1-trace.html")
        );

        boolean result = GitHubActionsSummary.appendSummary(config, timings, nested);

        assertTrue(result);
        assertTrue(nested.exists());
        nested.delete();
        nested.getParentFile().delete();
    }

    // ==========================================================
    // export (Environment detection)
    // ==========================================================

    @Test
    public void export_outsideGitHubActions_noOp() {
        // Unless GITHUB_ACTIONS=true is present in the OS environment, export must return false
        if (System.getenv("GITHUB_ACTIONS") == null && System.getProperty("seleniumboot.github.step.summary") == null) {
            SeleniumBootConfig config = configWithTracing(true);
            List<TestTiming> timings = List.of(
                    timing("t1", "ClassOne", "FAILED", "Fail 1", "traces/ClassOne/t1-trace.html")
            );
            boolean result = GitHubActionsSummary.export(config, timings);
            assertFalse(result, "export must no-op outside GitHub Actions");
        }
    }

    @Test
    public void export_withSystemPropertyOverride_writesToFile() throws IOException {
        String propKey = "seleniumboot.github.step.summary";
        String previous = System.getProperty(propKey);
        try {
            System.setProperty(propKey, tempSummaryFile.getAbsolutePath());
            SeleniumBootConfig config = configWithTracing(true);
            List<TestTiming> timings = List.of(
                    timing("t1", "ClassOne", "FAILED", "Fail in Actions", "traces/ClassOne/t1-trace.html")
            );

            boolean result = GitHubActionsSummary.export(config, timings);

            assertTrue(result, "export should succeed when summary path property is provided");
            String content = Files.readString(tempSummaryFile.toPath());
            assertTrue(content.contains("### Failed Tests"));
            assertTrue(content.contains("Fail in Actions"));
        } finally {
            if (previous != null) {
                System.setProperty(propKey, previous);
            } else {
                System.clearProperty(propKey);
            }
        }
    }
}

