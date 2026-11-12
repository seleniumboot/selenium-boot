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
            assertTrue(html(1, 0).contains("const trendData = [];"));
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
    public void copyFailureButton_onlyOnFailedTestsWithErrorDetail() throws Exception {
        String json = """
        {"totalTests":3,"passedTests":1,"failedTests":1,"skippedTests":1,"passRate":33.3,"tests":[
          {"testId":"a.A.ok","status":"PASSED","testLogicMs":1,"totalMs":1,"stepsNote":"x","description":"d","performanceMetrics":{"pageLoadMs":1}},
          {"testId":"a.A.bad<x>","status":"FAILED","testLogicMs":1,"totalMs":1,"errorMessage":"boom","stackTrace":"at a.b"},
          {"testId":"a.A.skip","status":"SKIPPED","testLogicMs":1,"totalMs":1,"errorMessage":"skipped because"}
        ]}""";
        String reportHtml = HtmlReportGenerator.buildHtml(new ObjectMapper().readTree(json));
        String body = reportHtml.substring(reportHtml.indexOf("<tbody"));
        assertFalse(body.contains("data-test-id=\"a.A.ok\""), "no button on passed tests");
        assertFalse(body.contains("data-test-id=\"a.A.skip\""), "no button on skipped tests");
        assertTrue(body.contains("data-test-id=\"a.A.bad&lt;x&gt;\""), "test id is escaped in the attribute");
        assertTrue(reportHtml.contains("function copyFailure("));
        assertTrue(reportHtml.contains("document.execCommand('copy')"), "file:// fallback present");
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

    // ── Sortable Columns in Test Cases Table (Issue #217) ──

    @Test
    public void buildHtml_sortableHeaders_haveSortableClassClickHandlersAndSortIcons() throws Exception {
        String reportHtml = html(1, 0);

        int tableStart = reportHtml.indexOf("<table id=\"testTable\">");
        int theadEnd = reportHtml.indexOf("</thead>", tableStart);
        assertTrue(tableStart > 0 && theadEnd > tableStart, "testTable thead must be present in HTML");
        String theadHtml = reportHtml.substring(tableStart, theadEnd);

        // 1. Test column header
        assertTrue(theadHtml.contains("sortTestTable('test')"),
                "Test column header must contain sortTestTable('test') onclick handler");
        assertTrue(theadHtml.contains("data-sort-key=\"test\""),
                "Test column header must carry data-sort-key=\"test\"");

        // 2. Status column header
        assertTrue(theadHtml.contains("sortTestTable('status')"),
                "Status column header must contain sortTestTable('status') onclick handler");
        assertTrue(theadHtml.contains("data-sort-key=\"status\""),
                "Status column header must carry data-sort-key=\"status\"");

        // 3. Logic (ms) column header
        assertTrue(theadHtml.contains("sortTestTable('logic')"),
                "Logic ms column header must contain sortTestTable('logic') onclick handler");
        assertTrue(theadHtml.contains("data-sort-key=\"logic\""),
                "Logic ms column header must carry data-sort-key=\"logic\"");

        // 4. Total (ms) column header
        assertTrue(theadHtml.contains("sortTestTable('total')"),
                "Total ms column header must contain sortTestTable('total') onclick handler");
        assertTrue(theadHtml.contains("data-sort-key=\"total\""),
                "Total ms column header must carry data-sort-key=\"total\"");

        // 5. Check sortable class and sort-icon presence
        int sortableClassCount = 0;
        int idx = 0;
        while ((idx = theadHtml.indexOf("class=\"sortable\"", idx)) >= 0) {
            sortableClassCount++;
            idx += "class=\"sortable\"".length();
        }
        assertEquals(sortableClassCount, 4, "There should be exactly 4 sortable column headers");

        int sortIconCount = 0;
        idx = 0;
        while ((idx = theadHtml.indexOf("class=\"sort-icon\"", idx)) >= 0) {
            sortIconCount++;
            idx += "class=\"sort-icon\"".length();
        }
        assertEquals(sortIconCount, 4, "There should be exactly 4 sort-icon elements in the thead");
    }

    @Test
    public void buildHtml_nonSortableHeaders_doNotCarrySortControls() throws Exception {
        String reportHtml = html(1, 0);

        int tableStart = reportHtml.indexOf("<table id=\"testTable\">");
        int theadEnd = reportHtml.indexOf("</thead>", tableStart);
        assertTrue(tableStart > 0 && theadEnd > tableStart, "testTable thead must be present in HTML");
        String theadHtml = reportHtml.substring(tableStart, theadEnd);

        // Split into <th> chunks to inspect non-sortable columns individually
        String[] ths = theadHtml.split("<th");
        for (String th : ths) {
            if (th.contains(">Description<") || th.contains(">Screenshot<") || th.contains(">Detail<")) {
                assertFalse(th.contains("sortable"), "Non-sortable column must not have sortable class: " + th);
                assertFalse(th.contains("sortTestTable"), "Non-sortable column must not have sort onclick: " + th);
                assertFalse(th.contains("sort-icon"), "Non-sortable column must not have sort-icon span: " + th);
            }
        }
    }

    @Test
    public void buildHtml_testRows_carryLogicAndTotalMsDataAttributes() throws Exception {
        String json = """
        {
            "totalTests": 2,
            "passedTests": 1,
            "failedTests": 1,
            "skippedTests": 0,
            "passRate": 50.0,
            "tests": [
                {
                    "testId": "com.example.OrderTest.checkoutSuccess",
                    "testClassName": "OrderTest",
                    "status": "PASSED",
                    "testLogicMs": 1420,
                    "totalMs": 2850
                },
                {
                    "testId": "com.example.OrderTest.paymentFailure",
                    "testClassName": "OrderTest",
                    "status": "FAILED",
                    "testLogicMs": 0,
                    "totalMs": 500,
                    "errorMessage": "gateway timeout"
                }
            ]
        }
        """;
        JsonNode root = new ObjectMapper().readTree(json);
        String reportHtml = HtmlReportGenerator.buildHtml(root);

        // Passed test row assertions
        assertTrue(reportHtml.contains("data-test=\"checkoutsuccess\""), "methodName lowercased in data-test");
        assertTrue(reportHtml.contains("data-status=\"PASSED\""), "status attribute present");
        assertTrue(reportHtml.contains("data-logic-ms=\"1420\""), "data-logic-ms must match testLogicMs");
        assertTrue(reportHtml.contains("data-total-ms=\"2850\""), "data-total-ms must match totalMs");

        // Failed test row assertions
        assertTrue(reportHtml.contains("data-test=\"paymentfailure\""), "methodName lowercased in data-test");
        assertTrue(reportHtml.contains("data-status=\"FAILED\""), "status attribute present");
        assertTrue(reportHtml.contains("data-logic-ms=\"0\""), "data-logic-ms handles 0 cleanly");
        assertTrue(reportHtml.contains("data-total-ms=\"500\""), "data-total-ms must match totalMs");
    }

    @Test
    public void buildHtml_sortingScript_includesSortingLogicAndFilterIntegration() throws Exception {
        String reportHtml = html(1, 0);

        int scriptStart = reportHtml.lastIndexOf("<script>");
        int scriptEnd = reportHtml.lastIndexOf("</script>");
        assertTrue(scriptStart > 0 && scriptEnd > scriptStart, "Generated HTML must contain embedded script block");
        String js = reportHtml.substring(scriptStart, scriptEnd);

        // 1. Function and state tracking
        assertTrue(js.contains("function sortTestTable(") || js.contains("sortTestTable = function("),
                "Report script must define sortTestTable function");
        assertTrue(js.contains("currentSortCol") && js.contains("currentSortDir"),
                "Report script must track currentSortCol and currentSortDir");

        // 2. Supported sort keys
        assertTrue(js.contains("'test'") && js.contains("'status'") && js.contains("'logic'") && js.contains("'total'"),
                "Sorting logic must support 'test', 'status', 'logic', and 'total' keys");

        // 3. Numeric attributes and comparison
        assertTrue(js.contains("data-logic-ms") && js.contains("data-total-ms"),
                "Sorting logic must query data-logic-ms and data-total-ms attributes");

        // 4. Sort indicator styling update
        assertTrue(js.contains(".sort-icon") && js.contains("'asc'") && js.contains("'desc'"),
                "Sorting logic must toggle asc and desc classes on .sort-icon elements");

        // 5. Group preservation and detail row pairing
        assertTrue(js.contains("detail-row"),
                "Sorting logic must maintain pairing with contiguous detail-row elements");
        assertTrue(js.contains("group-header") || js.contains("data-group"),
                "Sorting logic must preserve class group headers");

        // 6. Filter cooperation
        assertTrue(js.contains("applySearch()"),
                "sortTestTable must call applySearch() to refresh visibility under active filters");
    }

    @Test
    public void buildHtml_matrixRun_preservesSortingDataAttributesAndBrowserColumn() throws Exception {
        String json = """
        {
            "totalTests": 2,
            "passedTests": 2,
            "failedTests": 0,
            "skippedTests": 0,
            "passRate": 100.0,
            "tests": [
                {
                    "testId": "com.example.MatrixTest.testCrossBrowser",
                    "testClassName": "MatrixTest",
                    "browser": "chrome",
                    "status": "PASSED",
                    "testLogicMs": 850,
                    "totalMs": 1200
                },
                {
                    "testId": "com.example.MatrixTest.testCrossBrowser",
                    "testClassName": "MatrixTest",
                    "browser": "firefox",
                    "status": "PASSED",
                    "testLogicMs": 950,
                    "totalMs": 1350
                }
            ]
        }
        """;
        JsonNode root = new ObjectMapper().readTree(json);
        String reportHtml = HtmlReportGenerator.buildHtml(root);

        // Verify matrix groups
        assertTrue(reportHtml.contains("MatrixTest [Chrome]"), "Matrix run must include browser in group header");
        assertTrue(reportHtml.contains("MatrixTest [Firefox]"), "Matrix run must include browser in group header");

        // Verify browser cells
        assertTrue(reportHtml.contains("<th>Browser</th>"), "Matrix run must include <th>Browser</th> in thead");
        assertTrue(reportHtml.contains("<td class=\"browser-cell\">Chrome</td>"));
        assertTrue(reportHtml.contains("<td class=\"browser-cell\">Firefox</td>"));

        // Verify sorting data attributes are present on both matrix rows
        assertTrue(reportHtml.contains("data-logic-ms=\"850\""));
        assertTrue(reportHtml.contains("data-total-ms=\"1200\""));
        assertTrue(reportHtml.contains("data-logic-ms=\"950\""));
        assertTrue(reportHtml.contains("data-total-ms=\"1350\""));

        // Verify table still contains sortable headers
        assertTrue(reportHtml.contains("onclick=\"sortTestTable('test')\""));
        assertTrue(reportHtml.contains("onclick=\"sortTestTable('logic')\""));
    }

    @Test
    public void buildHtml_sortableHeaderCss_includesSortIconsAndActiveStates() throws Exception {
        String reportHtml = html(1, 0);

        int styleStart = reportHtml.indexOf("<style>");
        int styleEnd = reportHtml.indexOf("</style>");
        assertTrue(styleStart > 0 && styleEnd > styleStart, "Report must contain embedded style block");
        String css = reportHtml.substring(styleStart, styleEnd);

        assertTrue(css.contains(".sortable"), "CSS must contain .sortable selector");
        assertTrue(css.contains(".sort-icon"), "CSS must contain .sort-icon selector");
        assertTrue(css.contains(".sort-icon.asc") || css.contains(".asc"), "CSS must support asc sort icon state");
        assertTrue(css.contains(".sort-icon.desc") || css.contains(".desc"), "CSS must support desc sort icon state");
    }

    @Test
    public void generate_writesValidReportWithSortableColumnsToDisk() throws Exception {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("sb-e2e-report");
        String originalReportsDir = System.getProperty("seleniumboot.reports.dir");
        System.setProperty("seleniumboot.reports.dir", tempDir.toString());
        try {
            // Write a sample metrics JSON file
            String sampleMetricsJson = """
            {
                "totalTests": 2,
                "passedTests": 1,
                "failedTests": 1,
                "skippedTests": 0,
                "passRate": 50.0,
                "suiteDurationMs": 2500,
                "tests": [
                    {
                        "testId": "com.example.SampleTest.testAlpha",
                        "testClassName": "SampleTest",
                        "status": "PASSED",
                        "testLogicMs": 500,
                        "totalMs": 800
                    },
                    {
                        "testId": "com.example.SampleTest.testBeta",
                        "testClassName": "SampleTest",
                        "status": "FAILED",
                        "testLogicMs": 1200,
                        "totalMs": 1700,
                        "errorMessage": "Assertion failed"
                    }
                ]
            }
            """;
            java.nio.file.Files.writeString(tempDir.resolve("selenium-boot-metrics.json"), sampleMetricsJson);

            // Execute generate()
            HtmlReportGenerator.generate();

            // Verify report file was created
            java.nio.file.Path reportPath = tempDir.resolve("selenium-boot-report.html");
            assertTrue(java.nio.file.Files.exists(reportPath), "selenium-boot-report.html must exist on disk");
            assertTrue(java.nio.file.Files.size(reportPath) > 0, "Report file must not be empty");

            // Read generated HTML and verify sortable columns and data attributes
            String generatedHtml = java.nio.file.Files.readString(reportPath);
            assertTrue(generatedHtml.contains("class=\"sortable\""), "Generated report on disk must contain sortable headers");
            assertTrue(generatedHtml.contains("sortTestTable('test')"), "Generated report on disk must contain test sort handler");
            assertTrue(generatedHtml.contains("sortTestTable('logic')"), "Generated report on disk must contain logic sort handler");
            assertTrue(generatedHtml.contains("data-logic-ms=\"500\""), "Generated report on disk must contain row data-logic-ms");
            assertTrue(generatedHtml.contains("data-total-ms=\"800\""), "Generated report on disk must contain row data-total-ms");
            assertTrue(generatedHtml.contains("data-logic-ms=\"1200\""), "Generated report on disk must contain row data-logic-ms");
            assertTrue(generatedHtml.contains("data-total-ms=\"1700\""), "Generated report on disk must contain row data-total-ms");
            assertTrue(generatedHtml.contains("function sortTestTable"), "Generated report on disk must contain sorting script");
        } finally {
            if (originalReportsDir != null) {
                System.setProperty("seleniumboot.reports.dir", originalReportsDir);
            } else {
                System.clearProperty("seleniumboot.reports.dir");
            }
        }
    }

    @Test
    public void downloadTestButton_presentInDetailRow() throws Exception {
        String json = """
        {"totalTests":3,"passedTests":1,"failedTests":1,"skippedTests":1,"passRate":33.3,"tests":[
          {"testId":"a.A.ok","status":"PASSED","testLogicMs":1,"totalMs":1,"stepsNote":"x","description":"d","performanceMetrics":{"pageLoadMs":1}},
          {"testId":"a.A.bad<x>","status":"FAILED","testLogicMs":1,"totalMs":1,"errorMessage":"boom","stackTrace":"at a.b"},
          {"testId":"a.A.skip","status":"SKIPPED","testLogicMs":1,"totalMs":1,"errorMessage":"skipped because"}
        ]}""";
        String reportHtml = HtmlReportGenerator.buildHtml(new ObjectMapper().readTree(json));
        String body = reportHtml.substring(reportHtml.indexOf("<tbody"));

        // Download button rendered with escaped test ID
        assertTrue(body.contains("class=\"download-test-btn\""), "download-test-btn must be present in detail row");
        assertTrue(body.contains("data-download-id=\"a.A.bad&lt;x&gt;\""), "test id is escaped in data-download-id");
        assertTrue(body.contains("data-download-id=\"a.A.ok\""), "download button available on passed test with detail");
        assertTrue(body.contains("data-download-id=\"a.A.skip\""), "download button available on skipped test with error detail");
        assertTrue(body.contains("onclick=\"downloadTest(this, event)\""), "download button must invoke downloadTest");

        // Existing copy failure button constraint is not broken
        assertFalse(body.contains("data-test-id=\"a.A.ok\""), "copy failure button only on failed tests");
    }

    @Test
    public void downloadTestScript_containsStandaloneExportLogic() throws Exception {
        String reportHtml = html(1, 0);

        // JavaScript functions present
        assertTrue(reportHtml.contains("function downloadTest("), "report must define downloadTest function");
        assertTrue(reportHtml.contains("function buildStandaloneTestHtml("), "report must define buildStandaloneTestHtml function");

        // Blob and download mechanics
        assertTrue(reportHtml.contains("new Blob("), "downloadTest must create a client-side Blob");
        assertTrue(reportHtml.contains("URL.createObjectURL("), "downloadTest must create an object URL for download");
        assertTrue(reportHtml.contains(".download ="), "downloadTest must set download attribute on anchor");

        // Theme fidelity
        assertTrue(reportHtml.contains("data-theme"), "buildStandaloneTestHtml must preserve data-theme");
        assertTrue(reportHtml.contains("document.querySelector('style')"), "buildStandaloneTestHtml must read inline CSS from style element");

        // Self-contained lightbox and offline script
        assertTrue(reportHtml.contains("function openLightbox("), "offline script must define lightbox support");
        assertTrue(reportHtml.contains("function toggleDark("), "offline script must define theme toggle");

        // Tag pills call filterByTag, which the standalone file doesn't define
        assertTrue(reportHtml.contains("function stripReportOnlyHandlers("), "export must neutralise report-only handlers");
        assertTrue(reportHtml.contains("stripReportOnlyHandlers(testRowClone)"), "test row clone must be stripped");
        assertTrue(reportHtml.contains("stripReportOnlyHandlers(detailRowClone)"), "detail row clone must be stripped");
    }

    @Test
    public void downloadTestCss_containsButtonAndTableCardRules() throws Exception {
        String reportHtml = html(1, 0);

        assertTrue(reportHtml.contains(".download-test-btn"), "report CSS must define .download-test-btn");
        assertTrue(reportHtml.contains(".table-card"), "report CSS must define .table-card");
        assertTrue(reportHtml.contains(".table-wrap"), "report CSS must define .table-wrap");
        assertTrue(reportHtml.contains(".detail-panel"), "report CSS must define .detail-panel");
    }

    @Test
    public void downloadTest_matrixRun_preservesBrowserDataAttribute() throws Exception {
        String json = """
        {
            "totalTests": 2,
            "passedTests": 1,
            "failedTests": 1,
            "skippedTests": 0,
            "passRate": 50.0,
            "tests": [
                {
                    "testId": "com.example.MatrixTest.loginChrome",
                    "status": "PASSED",
                    "browser": "chrome",
                    "testLogicMs": 100,
                    "totalMs": 200,
                    "performanceMetrics": {"pageLoadMs": 50}
                },
                {
                    "testId": "com.example.MatrixTest.loginFirefox",
                    "status": "FAILED",
                    "browser": "firefox",
                    "testLogicMs": 150,
                    "totalMs": 250,
                    "errorMessage": "Element not found"
                }
            ]
        }
        """;
        String reportHtml = HtmlReportGenerator.buildHtml(new ObjectMapper().readTree(json));

        assertTrue(reportHtml.contains("data-browser=\"Chrome\""), "Row should carry capitalized data-browser attribute for Chrome");
        assertTrue(reportHtml.contains("data-browser=\"Firefox\""), "Row should carry capitalized data-browser attribute for Firefox");
        assertTrue(reportHtml.contains("data-download-id=\"com.example.MatrixTest.loginChrome\""));
        assertTrue(reportHtml.contains("data-download-id=\"com.example.MatrixTest.loginFirefox\""));
    }

    @Test
    public void downloadTest_standaloneExport_isFullySelfContainedAndHasNoExternalFetches() throws Exception {
        String reportHtml = html(1, 0);

        // Verify report.css has no external network imports or URLs
        int styleStart = reportHtml.indexOf("<style>");
        int styleEnd = reportHtml.indexOf("</style>");
        String css = reportHtml.substring(styleStart, styleEnd);
        assertFalse(css.contains("http://"), "CSS must not contain external HTTP references");
        assertFalse(css.contains("https://"), "CSS must not contain external HTTPS references");
        assertFalse(css.contains("@import"), "CSS must not contain @import rules");

        // Verify standalone generator template in report.js does not fetch external resources
        int scriptStart = reportHtml.lastIndexOf("<script>");
        int scriptEnd = reportHtml.lastIndexOf("</script>");
        String js = reportHtml.substring(scriptStart, scriptEnd);
        assertFalse(js.contains("fetch("), "Export logic must not perform network fetch requests");
        assertFalse(js.contains("XMLHttpRequest"), "Export logic must not perform XHR requests");
        assertTrue(js.contains("new Blob("), "Export must generate file in-memory using client-side Blob");
        assertTrue(js.contains("buildStandaloneTestHtml"), "Export must include standalone HTML builder");
    }

    @Test
    public void trendData_linksOnlyRunsWhoseReportWasKept() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("sb-kept");
        System.setProperty("seleniumboot.history.dir", dir.toString());
        try {
            for (String ts : new String[]{"20261110-100000", "20261111-100000"}) {
                java.nio.file.Files.writeString(dir.resolve("selenium-boot-metrics-" + ts + ".json"), "{\"passRate\":50.0}");
            }
            java.nio.file.Files.writeString(dir.resolve("selenium-boot-report-20261111-100000.html"), "<html></html>");

            JsonNode trend = new ObjectMapper().readTree(HtmlReportGenerator.buildTrendData(20));
            assertFalse(trend.get(0).has("report"), "no kept report, no link");
            assertEquals(trend.get(1).get("report").asText(), "selenium-boot-report-20261111-100000.html");
        } finally {
            System.clearProperty("seleniumboot.history.dir");
        }
    }

    @Test
    public void keepReportCopy_writesCopyWithLocalLinks_andPrunesOldest() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("sb-keep");
        System.setProperty("seleniumboot.history.dir", dir.toString());
        try {
            String html = "<script>\nconst trendReportBase = \"../h/\";\nconst x = 1;\n</script>";
            HtmlReportGenerator.keepReportCopy(html, 2, "selenium-boot-metrics-20261110-100000.json");
            HtmlReportGenerator.keepReportCopy(html, 2, "selenium-boot-metrics-20261111-100000.json");
            HtmlReportGenerator.keepReportCopy(html, 2, "selenium-boot-metrics-20261112-100000.json");

            assertFalse(java.nio.file.Files.exists(dir.resolve("selenium-boot-report-20261110-100000.html")), "oldest pruned");
            java.nio.file.Path newest = dir.resolve("selenium-boot-report-20261112-100000.html");
            assertTrue(java.nio.file.Files.exists(dir.resolve("selenium-boot-report-20261111-100000.html")));
            String copy = java.nio.file.Files.readString(newest);
            assertTrue(copy.contains("const trendReportBase = \"\";\nconst x = 1;"), "copy links siblings by file name: " + copy);
        } finally {
            System.clearProperty("seleniumboot.history.dir");
        }
    }

    @Test
    public void keepReportCopy_offByDefault() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("sb-keep-off");
        System.setProperty("seleniumboot.history.dir", dir.toString());
        try {
            assertEquals(new SeleniumBootConfig.Flakiness().getKeepReports(), 0);
            HtmlReportGenerator.keepReportCopy("<html></html>", 0, "selenium-boot-metrics-20261110-100000.json");
            HtmlReportGenerator.keepReportCopy("<html></html>", 5, null);
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                assertEquals(files.count(), 0L);
            }
        } finally {
            System.clearProperty("seleniumboot.history.dir");
        }
    }

    @Test
    public void trendReportBase_isRelativeToTheReport() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("sb-base");
        System.setProperty("seleniumboot.reports.dir", root.resolve("target").toString());
        try {
            System.setProperty("seleniumboot.history.dir", root.resolve("target/metrics-history").toString());
            assertEquals(HtmlReportGenerator.trendReportBase(), "\"metrics-history/\"");
            System.setProperty("seleniumboot.history.dir", root.resolve(".selenium-boot-history").toString());
            assertEquals(HtmlReportGenerator.trendReportBase(), "\"../.selenium-boot-history/\"");
            assertTrue(html(1, 0).contains("const trendReportBase = \"../.selenium-boot-history/\";"));
        } finally {
            System.clearProperty("seleniumboot.history.dir");
            System.clearProperty("seleniumboot.reports.dir");
        }
    }
}
