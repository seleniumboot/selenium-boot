package com.seleniumboot.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class ReportTabBehaviourTest {

    private static String failureRows() throws Exception {
        JsonNode tests = new ObjectMapper().readTree("""
                [{"testClassName":"com.x.LoginTest","testName":"com.x.LoginTest.badLogin","status":"FAILED",
                  "errorMessage":"boom","stackTrace":"java.lang.AssertionError: boom\\n\\tat x"}]
                """);
        Method m = com.seleniumboot.reporting.HtmlReportGenerator.class
                .getDeclaredMethod("buildFailureRows", JsonNode.class, boolean.class);
        m.setAccessible(true);
        return (String) m.invoke(null, tests, false);
    }

    @Test
    public void failureRowsDoNotShareGroupIdWithTestCasesTable() throws Exception {
        // The Test Cases group toggle selects by data-group; sharing the id hid failure rows too.
        String html = failureRows();
        Assert.assertFalse(html.contains("data-group=\"com.x.LoginTest\""), html);
        Assert.assertTrue(html.contains("data-group=\"failures:com.x.LoginTest\""), html);
    }

    @Test
    public void failureDetailsStartCollapsedButRowsStayVisible() throws Exception {
        String html = failureRows();
        int detail = html.indexOf("class=\"detail-row");
        Assert.assertTrue(detail > 0, html);
        Assert.assertTrue(html.substring(detail, html.indexOf('>', detail)).contains("display:none"), html);
        int row = html.indexOf("class=\"group-member expandable\"");
        Assert.assertTrue(row > 0, html);
        Assert.assertFalse(html.substring(row, html.indexOf('>', row)).contains("display:none"), html);
        Assert.assertFalse(html.contains("expand-icon open"), html);
    }

    @Test
    public void reportAlwaysOpensOnDashboard() throws Exception {
        try (var in = getClass().getResourceAsStream("/report-template.html")) {
            String t = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertFalse(t.contains("sb-tab"), "report must not restore the last-open tab");
        }
    }

    @Test
    public void reportJsDoesNotContainSbTabKey() throws Exception {
        try (var in = getClass().getResourceAsStream("/report/report.js")) {
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertFalse(js.contains("sb-tab"), "report.js must not restore the last-open tab");
        }
    }

    @Test
    public void dashboardStatCardsAndDonutFilterTheTestCasesTab() throws Exception {
        String dashboard, js;
        try (var in = getClass().getResourceAsStream("/report/partials/dashboard.html")) {
            dashboard = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (var in = getClass().getResourceAsStream("/report/report.js")) {
            js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String status : new String[] {"PASSED", "FAILED", "SKIPPED"}) {
            Assert.assertTrue(dashboard.contains("onclick=\"filterByStatus('" + status + "')\""), status);
        }
        Assert.assertTrue(js.contains("function filterByStatus(status)"), js);
        Assert.assertTrue(js.contains("filterByStatus(['PASSED', 'FAILED', 'SKIPPED'][els[0].index])"),
                "donut slice order must match its labels");
    }

    @Test
    public void reportJsUpdatesChartsOnThemeToggle() throws Exception {
        try (var in = getClass().getResourceAsStream("/report/report.js")) {
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertTrue(js.contains("function updateChartsTheme()"), "report.js must define updateChartsTheme()");
            Assert.assertTrue(js.contains("updateChartsTheme();"), "toggleDark must call updateChartsTheme()");
            Assert.assertTrue(js.contains("Chart.defaults.color = color('--text-secondary')"), "Chart defaults must use --text-secondary");
            Assert.assertTrue(js.contains("Chart.defaults.borderColor = color('--border')"), "Chart defaults must use --border");
            Assert.assertTrue(js.contains("donutChart.data.datasets[0].borderColor = surfaceColor"), "donut border must update to surfaceColor");
            Assert.assertTrue(js.contains("donutChart.update()"), "donutChart must be updated on theme change");
            Assert.assertTrue(js.contains("executionChart.update()"), "executionChart must be updated on theme change");
            Assert.assertTrue(js.contains("updateTrendLine(trendRateChart"), "trendRateChart must update on theme change");
            Assert.assertTrue(js.contains("updateTrendLine(trendTimeChart"), "trendTimeChart must update on theme change");
            Assert.assertTrue(js.contains("updateTrendLine(trendFlakyChart"), "trendFlakyChart must update on theme change");
        }
    }

    @Test
    public void donutChartBorderUsesSurfaceColor() throws Exception {
        try (var in = getClass().getResourceAsStream("/report/report.js")) {
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertFalse(js.contains("borderColor: '#fff'"), "donut chart must not hardcode #fff border");
            Assert.assertTrue(js.contains("borderColor: color('--surface')"), "donut chart must use --surface for border");
        }
    }
}
