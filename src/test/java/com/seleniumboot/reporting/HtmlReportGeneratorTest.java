package com.seleniumboot.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    public void generatedReportEmbedsAssetsForOfflineUse() throws Exception {
        String html = html(1, 0);
        assertTrue(html.contains("--primary: #303f9f"));
        assertTrue(html.contains("function showTab(tabId, el)"));
        assertTrue(html.contains("Chart"));
        assertFalse(html.contains("<script src="));
        assertFalse(html.contains("<link href="));
    }

    @Test
    public void generatedReportHasBalancedStructuralTags() throws Exception {
        String html = html(1, 0);
        assertEquals(countTag(html, "nav", false), countTag(html, "nav", true));
        assertEquals(countTag(html, "main", false), countTag(html, "main", true));
        assertEquals(countTag(html, "div", false), countTag(html, "div", true));
    }

    private static int countTag(String html, String tag, boolean closing) {
        String marker = closing ? "</" + tag : "<" + tag;
        int count = 0;
        int index = 0;
        while ((index = html.indexOf(marker, index)) >= 0) {
            count++;
            index += marker.length();
        }
        return count;
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

    @Test
    public void trendData_isChronological_andLimitedToLastRuns() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("sb-history");
        System.setProperty("seleniumboot.history.dir", dir.toString());
        try {
            for (int i = 1; i <= 3; i++) {
                java.nio.file.Files.writeString(dir.resolve("selenium-boot-metrics-2026101" + i + "-10300" + i + ".json"),
                        "{\"passRate\":" + (i * 30) + ".0,\"failedTests\":" + i + ",\"flakyTests\":" + i + ",\"suiteDurationMs\":" + (i * 2000) + "}");
            }
            java.nio.file.Files.writeString(dir.resolve("selenium-boot-metrics-20261019-103000.json"), "not json");

            JsonNode trend = new ObjectMapper().readTree(HtmlReportGenerator.buildTrendData(2));
            assertEquals(trend.size(), 1, "last 2 files taken, the unreadable one skipped");
            assertEquals(trend.get(0).get("passRate").asDouble(), 90.0);
            assertEquals(trend.get(0).get("durationSec").asDouble(), 6.0);
            assertEquals(trend.get(0).get("flakyTests").asInt(), 3, "flakyTests carried into trend entry");
            assertEquals(trend.get(0).get("label").asText(), "10-13 10:30");

            JsonNode all = new ObjectMapper().readTree(HtmlReportGenerator.buildTrendData(20));
            assertEquals(all.size(), 3);
            assertEquals(all.get(0).get("passRate").asDouble(), 30.0, "oldest first");
            assertEquals(all.get(0).get("flakyTests").asInt(), 1, "flakyTests on oldest run");
            assertEquals(all.get(2).get("flakyTests").asInt(), 3, "flakyTests on newest run");
        } finally {
            System.clearProperty("seleniumboot.history.dir");
        }
    }

    @Test
    public void trendData_emptyWithoutHistory() throws Exception {
        System.setProperty("seleniumboot.history.dir",
                java.nio.file.Files.createTempDirectory("sb-empty").toString());
        try {
            assertEquals(HtmlReportGenerator.buildTrendData(20), "[]");
            assertTrue(html(1, 0).contains("const trendData = JSON.parse('[]');"));
        } finally {
            System.clearProperty("seleniumboot.history.dir");
        }
    }

    @Test
    public void aiRunSummary_rendersEscaped_andIsAbsentWithout() throws Exception {
        assertFalse(html(1, 0).contains("AI Run Summary"));
        JsonNode root = new ObjectMapper().readTree(
                "{\"totalTests\":1,\"failedTests\":1,\"passRate\":0.0,\"tests\":[],"
                + "\"aiRunSummary\":\"Most failures share <b>one</b> cause.\"}");
        String html = HtmlReportGenerator.buildHtml(root);
        assertTrue(html.contains("AI Run Summary"));
        assertTrue(html.contains("Most failures share &lt;b&gt;one&lt;/b&gt; cause."), "summary text is HTML-escaped");
    }

    @Test
    public void resultsByTag_omittedWhenNoTags() throws Exception {
        String reportHtml = html(3, 0);
        assertFalse(reportHtml.contains("<span>Results by Tag</span>"), "card must be omitted when no tests have tags");
        assertFalse(reportHtml.contains("id=\"tagResultsCard\""));
    }

    @Test
    public void resultsByTag_renderedWhenTagsPresent() throws Exception {
        String json = """
{
            "totalTests": 3,
            "passedTests": 2,
            "failedTests": 1,
            "skippedTests": 0,
            "passRate": 66.7,
            "tests": [
                {
                    "testId": "com.example.LoginTest.validLogin",
                    "testClassName": "LoginTest",
                    "status": "PASSED",
                    "tags": ["smoke", "auth"],
                    "testLogicMs": 100,
                    "totalMs": 150
                },
                {
                    "testId": "com.example.LoginTest.invalidLogin",
                    "testClassName": "LoginTest",
                    "status": "FAILED",
                    "tags": ["auth"],
                    "testLogicMs": 200,
                    "totalMs": 250,
                    "errorMessage": "login failed"
                },
                {
                    "testId": "com.example.CheckoutTest.placeOrder",
                    "testClassName": "CheckoutTest",
                    "status": "PASSED",
                    "tags": ["smoke"],
                    "testLogicMs": 300,
                    "totalMs": 350
                }
            ]
        }""";
        JsonNode root = new ObjectMapper().readTree(json);
        String reportHtml = HtmlReportGenerator.buildHtml(root);

        // 1. Dashboard Results by Tag card
        assertTrue(reportHtml.contains("id=\"tagResultsCard\""), "tag results card must be present");
        assertTrue(reportHtml.contains("Results by Tag"));
        assertTrue(reportHtml.contains("2 tags"), "two unique tags: auth and smoke");

        // 2. Auth tag stats: 2 total, 1 passed, 1 failed -> 50.0%
        assertTrue(reportHtml.contains("data-tag=\"auth\""));
        assertTrue(reportHtml.contains("onclick=\"filterByTag(this.dataset.tag, event)\""));
        assertTrue(reportHtml.contains("50.0%"));

        // 3. Smoke tag stats: 2 total, 2 passed -> 100.0%
        assertTrue(reportHtml.contains("data-tag=\"smoke\""));
        assertTrue(reportHtml.contains("100.0%"));

        // 4. Test Cases table rows have data-tags and chips
        assertTrue(reportHtml.contains("data-tags=\"smoke,auth\""));
        assertTrue(reportHtml.contains("data-tags=\"auth\""));
        assertTrue(reportHtml.contains("data-tags=\"smoke\""));
        assertTrue(reportHtml.contains("class=\"tag-chip\""));

        // 5. Active tag filter pill is present in the markup
        assertTrue(reportHtml.contains("id=\"activeTagPill\""));
    }


    @Test
    public void buildHtml_tagWithQuote_renderedSafelyWithDataTag() throws Exception {
        String json = """
        {
            "totals": { "total": 1, "passed": 1, "failed": 0, "skipped": 0 },
            "tests": [
                {
                    "testId": "com.example.SpecialTagTest.testQuoteTag",
                    "testClassName": "SpecialTagTest",
                    "status": "PASSED",
                    "tags": ["it's", "x');alert(1);//"],
                    "testLogicMs": 100,
                    "totalMs": 120
                }
            ]
        }
        """;
        JsonNode root = new ObjectMapper().readTree(json);
        String reportHtml = HtmlReportGenerator.buildHtml(root);

        // Tags with quotes and injection payloads must be safely placed in data-tag attribute
        assertTrue(reportHtml.contains("data-tag=\"it's\""), "tag with quote must be safely in data-tag");
        assertTrue(reportHtml.contains("data-tag=\"x');alert(1);//\""), "tag with JS payload must be safely in data-tag");
        assertTrue(reportHtml.contains("onclick=\"filterByTag(this.dataset.tag, event)\""));

        // Must NOT interpolate quotes into inline JavaScript calls
        assertFalse(reportHtml.contains("filterByTag('it's'"));
        assertFalse(reportHtml.contains("filterByTag('x');alert(1);//'"));
    }

    @Test
    public void buildHtml_statusFilterPersistence_persistsAndRestoresFilter() throws Exception {
        String reportHtml = html(1, 0);

        // 1. Canonical storage key
        assertTrue(reportHtml.contains("sb-status-filter"),
                "Report must use the canonical 'sb-status-filter' storage key");

        // 2. setFilter persistence and removal on 'ALL'
        assertTrue(reportHtml.contains("localStorage.setItem('sb-status-filter'"),
                "setFilter must persist non-ALL status filter to localStorage");
        assertTrue(reportHtml.contains("localStorage.removeItem('sb-status-filter'"),
                "setFilter must remove stored status filter when 'ALL' is selected");

        // 3. Status filter restoration on page load
        assertTrue(reportHtml.contains("localStorage.getItem('sb-status-filter'"),
                "Report initialization must retrieve persisted status from localStorage");
        assertTrue(reportHtml.contains("['PASSED', 'FAILED', 'SKIPPED', 'FLAKY']"),
                "Status restoration must validate against allowed status values");
        assertTrue(reportHtml.contains(".filter-btn"),
                "Status restoration must update the active class on filter buttons");
        assertTrue(reportHtml.contains("activeFilter"),
                "Status restoration must set activeFilter variable");
    }

    @Test
    public void buildHtml_tagFilterPersistence_persistsAndClearsFilter() throws Exception {
        String reportHtml = html(1, 0);

        // 1. Canonical storage key
        assertTrue(reportHtml.contains("sb-tag-filter"),
                "Report must use the canonical 'sb-tag-filter' storage key");

        // 2. filterByTag persistence & clearTagFilter removal
        assertTrue(reportHtml.contains("localStorage.setItem('sb-tag-filter'"),
                "filterByTag must persist selected tag to localStorage");
        assertTrue(reportHtml.contains("localStorage.removeItem('sb-tag-filter'"),
                "clearTagFilter must remove stored tag filter from localStorage");

        // 3. Tag filter restoration on page load
        assertTrue(reportHtml.contains("localStorage.getItem('sb-tag-filter'"),
                "Report initialization must retrieve persisted tag from localStorage");
        assertTrue(reportHtml.contains("activeTagPill"),
                "Tag restoration must manage activeTagPill visibility");
        assertTrue(reportHtml.contains("activeTagName"),
                "Tag restoration must update activeTagName display");
    }

    @Test
    public void buildHtml_tagFilterRestoration_guardsAgainstMissingTags() throws Exception {
        String reportHtml = html(1, 0);

        // Verify missing tag validation inspects table rows before applying filter
        assertTrue(reportHtml.contains("#testTable tbody tr.group-member"),
                "Tag restoration must query test table member rows to locate matching tags");
        assertTrue(reportHtml.contains("data-tags"),
                "Tag restoration must inspect data-tags on table rows before activating filter");
        assertTrue(reportHtml.contains("tagFound"),
                "Tag restoration must track whether the stored tag actually exists in the report");
    }

    @Test
    public void buildHtml_filterStorageAccess_isGuardedByTryCatch() throws Exception {
        String reportHtml = html(1, 0);

        int scriptStart = reportHtml.lastIndexOf("<script>");
        int scriptEnd = reportHtml.lastIndexOf("</script>");
        assertTrue(scriptStart > 0 && scriptEnd > scriptStart, "Generated HTML must contain embedded script block");
        String js = reportHtml.substring(scriptStart, scriptEnd);

        // General error guarding
        assertTrue(js.contains("try {") || js.contains("try{"), "Storage access must use try blocks");
        assertTrue(js.contains("catch (e)") || js.contains("catch(e)") || js.contains("catch (err)"),
                "Storage access must catch storage exceptions");

        // Specific function error handling checks
        assertTrue(js.matches("(?s).*function setFilter[^{]*\\{.*?try\\s*\\{.*?localStorage\\.(setItem|removeItem)\\('sb-status-filter'.*?\\}\\s*catch.*"),
                "setFilter localStorage writes must be enclosed in try/catch");
        assertTrue(js.matches("(?s).*function filterByTag[^{]*\\{.*?try\\s*\\{.*?localStorage\\.(setItem|removeItem)\\('sb-tag-filter'.*?\\}\\s*catch.*"),
                "filterByTag localStorage writes must be enclosed in try/catch");
        assertTrue(js.matches("(?s).*function clearTagFilter[^{]*\\{.*?try\\s*\\{.*?localStorage\\.removeItem\\('sb-tag-filter'.*?\\}\\s*catch.*"),
                "clearTagFilter localStorage removal must be enclosed in try/catch");
        assertTrue(js.matches("(?s).*\\(function\\(\\)\\s*\\{.*?try\\s*\\{.*?sb-status-filter.*?sb-tag-filter.*?\\}\\s*catch.*"),
                "Filter restoration routine on load must be enclosed in try/catch");
    }

    @Test
    public void buildHtml_filterRestoration_preservesDashboardTabWithoutTabPersistence() throws Exception {
        String reportHtml = html(1, 0);

        // Default markup states
        assertTrue(reportHtml.contains("id=\"tab-dashboard\" class=\"tab-pane active\""),
                "Dashboard tab pane must be active by default");
        assertTrue(reportHtml.contains("nav-item active\" onclick=\"showTab('dashboard', this)\""),
                "Dashboard navigation item must be active by default");

        // Must not introduce any tab persistence key
        assertFalse(reportHtml.contains("sb-tab"),
                "Generated report must not introduce sb-tab persistence keys");

        // Filter restoration block must not switch tabs
        int scriptStart = reportHtml.lastIndexOf("<script>");
        String js = reportHtml.substring(scriptStart);
        int restoreBlock = js.indexOf("sb-status-filter");
        assertTrue(restoreBlock > 0, "Filter restoration script must exist");
        String restorationScript = js.substring(restoreBlock);
        assertFalse(restorationScript.contains("showTab('tests'"),
                "Filter restoration must not call showTab('tests') on load");
    }
}
