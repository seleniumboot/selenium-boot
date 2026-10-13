package com.seleniumboot.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.metrics.ExecutionMetrics;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** exportToJson() writes {@code testStatus}, comparing against history without counting itself. */
public class TestStatusExportTest {

    private Path dir;
    private String previousProperty;

    @BeforeMethod
    public void setUp() throws Exception {
        ExecutionMetrics.reset();
        dir = Files.createTempDirectory("status-export");
        previousProperty = System.getProperty("seleniumboot.reports.dir");
        System.setProperty("seleniumboot.reports.dir", dir.toString());
        Path history = Files.createDirectories(dir.resolve("metrics-history"));
        Path earlier = history.resolve("earlier.json");
        Files.writeString(earlier, "{\"tests\":[{\"testId\":\"p.A.fresh\",\"status\":\"PASSED\"},"
                + "{\"testId\":\"p.A.steady\",\"status\":\"PASSED\"}]}");
        Assert.assertTrue(earlier.toFile().setLastModified(1000));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ExecutionMetrics.reset();
        if (previousProperty == null) System.clearProperty("seleniumboot.reports.dir");
        else System.setProperty("seleniumboot.reports.dir", previousProperty);
        File[] history = dir.resolve("metrics-history").toFile().listFiles();
        if (history != null) for (File f : history) f.delete();
        Files.deleteIfExists(dir.resolve("metrics-history"));
        Files.deleteIfExists(dir.resolve("selenium-boot-metrics.json"));
        Files.deleteIfExists(dir);
    }

    private static void record(String id, String status, int retries) {
        ExecutionMetrics.markStart(id);
        for (int i = 0; i < retries; i++) ExecutionMetrics.recordRetry(id);
        ExecutionMetrics.recordStatus(id, status);
        ExecutionMetrics.markEnd(id);
    }

    private JsonNode export() throws Exception {
        ExecutionMetrics.exportToJson();
        return new ObjectMapper().readTree(dir.resolve("selenium-boot-metrics.json").toFile()).path("tests");
    }

    private static String statusOf(JsonNode tests, String id) {
        for (JsonNode t : tests) {
            if (id.equals(t.path("testId").asText())) return t.path("testStatus").asText(null);
        }
        throw new AssertionError("no test " + id);
    }

    @Test
    public void exportedMetrics_carryStatusFromRetriesAndHistory() throws Exception {
        record("p.A.fresh", "FAILED", 0);
        record("p.A.steady", "PASSED", 0);
        record("p.A.retried", "PASSED", 1);

        JsonNode tests = export();

        Assert.assertEquals(statusOf(tests, "p.A.fresh"), "NEWLY_FAILING");
        Assert.assertNull(statusOf(tests, "p.A.steady"));
        Assert.assertEquals(statusOf(tests, "p.A.retried"), "PASSED_ON_RETRY");
    }

    @Test
    public void exportingTwiceInOneRun_doesNotCompareTheRunWithItself() throws Exception {
        record("p.A.fresh", "FAILED", 0);
        export();
        // The first export left a copy of this run in the history directory.
        Assert.assertEquals(statusOf(export(), "p.A.fresh"), "NEWLY_FAILING");
    }
}
