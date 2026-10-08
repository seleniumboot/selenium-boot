package com.seleniumboot.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/** The dashboard leads with results; run configuration is a one-line summary plus a collapsed block. */
public class HtmlReportGeneratorTest {

    @BeforeMethod
    public void initContext() throws Exception {
        resetContext();
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Browser browser = new SeleniumBootConfig.Browser();
        browser.setName("chrome");
        browser.setHeadless(true);
        config.setBrowser(browser);
        SeleniumBootConfig.Execution execution = new SeleniumBootConfig.Execution();
        execution.setMode("local");
        execution.setThreadCount(4);
        config.setExecution(execution);
        config.setTimeouts(new SeleniumBootConfig.Timeouts());
        SeleniumBootContext.initialize(config);
    }

    @AfterMethod
    public void resetContext() throws Exception {
        Field f = SeleniumBootContext.class.getDeclaredField("CONFIG");
        f.setAccessible(true);
        ((AtomicReference<?>) f.get(null)).set(null);
    }

    private static String html(int passed, int failed) throws Exception {
        JsonNode root = new ObjectMapper().readTree(
                "{\"totalTests\":" + (passed + failed) + ",\"passedTests\":" + passed
                + ",\"failedTests\":" + failed + ",\"skippedTests\":0,\"passRate\":100.0,\"tests\":[]}");
        return HtmlReportGenerator.buildHtml(root);
    }

    @Test
    public void statsComeBeforeRunConfiguration() throws Exception {
        String html = html(3, 0);
        int stats = html.indexOf("class=\"card stat-card passed-card\"");
        int config = html.indexOf("Run configuration");
        assertTrue(stats > 0 && config > stats, "stat cards must precede the run configuration block");
    }

    @Test
    public void runConfigurationIsCollapsedByDefault() throws Exception {
        String html = html(3, 0);
        assertTrue(html.contains("<details class=\"card metadata-card\""));
        assertFalse(html.contains("<details class=\"card metadata-card\" open"));
    }

    @Test
    public void summaryLineCarriesTheKeyFacts() throws Exception {
        String html = html(3, 0);
        assertTrue(html.contains("class=\"run-summary\""));
        assertTrue(html.contains("chrome (headless)"));
        assertTrue(html.contains("4 threads"));
    }

    @Test
    public void emptyRowsAreOmitted() throws Exception {
        String html = html(3, 0);
        assertFalse(html.contains("Grid URL"), "no grid configured -> no row");
        assertFalse(html.contains(">unknown<"));
    }

    @Test
    public void failuresShortcutOnlyWhenSomethingFailed() throws Exception {
        assertFalse(html(3, 0).contains("View failures"));
        assertTrue(html(2, 1).contains("View failures"));
    }

    @Test
    public void durationIsFormattedAsHhMmSs() {
        assertEquals(HtmlReportGenerator.formatDuration(0), "00:00:00");
        assertEquals(HtmlReportGenerator.formatDuration(59_999), "00:00:59");
        assertEquals(HtmlReportGenerator.formatDuration(3_661_000), "01:01:01");
        assertEquals(HtmlReportGenerator.formatDuration(100L * 3600_000), "100:00:00", "hours are not capped");
    }

    @Test
    public void reportShowsSuiteWallClockInHhMmSs() throws Exception {
        JsonNode root = new ObjectMapper().readTree(
                "{\"totalTests\":2,\"passedTests\":2,\"failedTests\":0,\"skippedTests\":0,"
                + "\"passRate\":100.0,\"totalTimeMs\":7200000,\"suiteDurationMs\":3661000,\"tests\":[]}");
        String html = HtmlReportGenerator.buildHtml(root);
        assertTrue(html.contains("Duration: <strong>01:01:01</strong>"));
        assertFalse(html.contains("7200000 ms"));
        assertTrue(html.contains("<div class=\"stat-value\">01:01:01</div>"), "stat tile shows suite time");
        assertFalse(html.contains("Avg Time (ms)"), "per-test average tile is gone");
    }
}
