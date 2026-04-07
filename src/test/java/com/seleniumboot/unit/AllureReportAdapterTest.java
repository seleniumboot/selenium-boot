package com.seleniumboot.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.reporting.AllureReportAdapter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link AllureReportAdapter}.
 *
 * Writes a synthetic metrics JSON to a temp file, runs the adapter,
 * then asserts the generated Allure result files are correct.
 */
public class AllureReportAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File ALLURE_RESULTS = new File("target/allure-results");
    private File metricsFile;

    @BeforeMethod
    public void setup() throws IOException {
        // Wipe allure-results so each test starts with an empty directory
        deleteDir(ALLURE_RESULTS);
        metricsFile = File.createTempFile("metrics", ".json");
    }

    @AfterMethod
    public void cleanup() {
        if (metricsFile != null) metricsFile.delete();
        deleteDir(ALLURE_RESULTS);
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /** Writes a minimal metrics JSON with one test entry. */
    private void writeMetrics(String status, String errorMessage, String stackTrace)
            throws IOException {
        String error = errorMessage != null
                ? "\"errorMessage\": \"" + errorMessage + "\","
                : "";
        String trace = stackTrace != null
                ? "\"stackTrace\": \"" + stackTrace + "\","
                : "";
        String json = "{"
                + "\"totalTests\": 1, \"passedTests\": 1, \"failedTests\": 0,"
                + "\"skippedTests\": 0, \"passRate\": 100.0,"
                + "\"flakyTests\": 0, \"recoveredTests\": 0,"
                + "\"totalTimeMs\": 1000,"
                + "\"tests\": [{"
                + "  \"testId\": \"myLoginTest\","
                + "  \"testClassName\": \"LoginTest\","
                + "  \"thread\": \"main\","
                + "  \"status\": \"" + status + "\","
                + "  \"retryCount\": 0,"
                + "  \"driverStartupMs\": 200,"
                + "  \"testLogicMs\": 800,"
                + "  \"totalMs\": 1000,"
                + "  \"description\": \"Login with valid creds\","
                + error
                + trace
                + "  \"steps\": ["
                + "    {\"name\": \"Open page\",    \"offsetMs\": 0,   \"status\": \"PASSED\"},"
                + "    {\"name\": \"Click submit\", \"offsetMs\": 500, \"status\": \"PASSED\"}"
                + "  ]"
                + "}]}";
        Files.writeString(metricsFile.toPath(), json);
    }

    private List<File> resultFiles() {
        File allureResults = new File("target/allure-results");
        List<File> files = new ArrayList<>();
        if (allureResults.exists()) {
            for (File f : allureResults.listFiles()) {
                if (f.getName().endsWith("-result.json")) files.add(f);
            }
        }
        return files;
    }

    private JsonNode firstResult() throws IOException {
        List<File> files = resultFiles();
        assertFalse(files.isEmpty(), "No result files generated in target/allure-results");
        // Pick any one result file (there may be others from other tests)
        // Find the one for myLoginTest
        for (File f : files) {
            JsonNode node = MAPPER.readTree(f);
            if ("myLoginTest".equals(node.path("name").asText())) return node;
        }
        fail("No result file found for myLoginTest");
        return null;
    }

    // ----------------------------------------------------------
    // Tests
    // ----------------------------------------------------------

    @Test
    public void generate_createsResultFileForEachTest() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        assertFalse(resultFiles().isEmpty(), "At least one result file must be written");
    }

    @Test
    public void generate_passedTest_hasPassedStatus() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        assertEquals(firstResult().path("status").asText(), "passed");
    }

    @Test
    public void generate_failedTest_hasFailedStatus() throws IOException {
        writeMetrics("FAILED", "Expected [Home] but found [Login]", null);
        new AllureReportAdapter().generate(metricsFile);

        assertEquals(firstResult().path("status").asText(), "failed");
    }

    @Test
    public void generate_skippedTest_hasSkippedStatus() throws IOException {
        writeMetrics("SKIPPED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        assertEquals(firstResult().path("status").asText(), "skipped");
    }

    @Test
    public void generate_failedTest_statusDetailsContainMessage() throws IOException {
        writeMetrics("FAILED", "Expected [Home] but found [Login]", null);
        new AllureReportAdapter().generate(metricsFile);

        JsonNode details = firstResult().path("statusDetails");
        assertFalse(details.isMissingNode(), "statusDetails must be present for failed tests");
        assertEquals(details.path("message").asText(), "Expected [Home] but found [Login]");
    }

    @Test
    public void generate_failedTest_statusDetailsContainTrace() throws IOException {
        writeMetrics("FAILED", "assertion failed",
                "java.lang.AssertionError: assertion failed\\n\\tat com.example.Test.run(Test.java:42)");
        new AllureReportAdapter().generate(metricsFile);

        JsonNode details = firstResult().path("statusDetails");
        assertTrue(details.path("trace").asText().contains("AssertionError"),
                "trace must contain stack trace content");
    }

    @Test
    public void generate_passedTest_noStatusDetails() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        assertTrue(firstResult().path("statusDetails").isMissingNode(),
                "passed tests must not have statusDetails");
    }

    @Test
    public void generate_resultHasRequiredFields() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        JsonNode result = firstResult();
        assertFalse(result.path("uuid").asText().isEmpty(),     "uuid must be present");
        assertFalse(result.path("name").asText().isEmpty(),     "name must be present");
        assertFalse(result.path("fullName").asText().isEmpty(), "fullName must be present");
        assertTrue(result.path("start").asLong() > 0,          "start timestamp must be positive");
        assertTrue(result.path("stop").asLong() > 0,           "stop timestamp must be positive");
    }

    @Test
    public void generate_resultHasSuiteLabel() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        JsonNode labels = firstResult().path("labels");
        boolean hasSuiteLabel = false;
        for (JsonNode label : labels) {
            if ("suite".equals(label.path("name").asText())) {
                assertEquals(label.path("value").asText(), "LoginTest");
                hasSuiteLabel = true;
            }
        }
        assertTrue(hasSuiteLabel, "result must have a 'suite' label");
    }

    @Test
    public void generate_resultHasSteps() throws IOException {
        writeMetrics("PASSED", null, null);
        new AllureReportAdapter().generate(metricsFile);

        JsonNode steps = firstResult().path("steps");
        assertEquals(steps.size(), 2, "Two steps from the metrics must appear in result");
        assertEquals(steps.get(0).path("name").asText(), "Open page");
        assertEquals(steps.get(1).path("name").asText(), "Click submit");
    }

    @Test
    public void generate_nonExistentMetricsFile_isNoOp() {
        // Must not throw
        new AllureReportAdapter().generate(new File("target/nonexistent-metrics.json"));
    }

    @Test
    public void getName_returnsAllure() {
        assertEquals(new AllureReportAdapter().getName(), "allure");
    }

    // ----------------------------------------------------------
    // Cleanup helper
    // ----------------------------------------------------------

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
