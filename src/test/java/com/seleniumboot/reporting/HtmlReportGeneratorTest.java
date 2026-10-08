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
}
