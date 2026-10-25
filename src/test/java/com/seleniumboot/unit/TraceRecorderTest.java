package com.seleniumboot.unit;

import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.steps.ConsoleEntry;
import com.seleniumboot.steps.LocatorUse;
import com.seleniumboot.steps.NetworkFailure;
import com.seleniumboot.steps.StepRecord;
import com.seleniumboot.tracing.TraceRecorder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit tests for {@link TraceRecorder} — HTML generation logic only.
 * No browser required.
 */
public class TraceRecorderTest {

    // ------------------------------------------------------------------
    // buildHtml — metadata
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_containsTestName() {
        TestTiming t = timing("MyTest");
        String html = TraceRecorder.buildHtml(t, "MyTest", null);
        Assert.assertTrue(html.contains("MyTest"), "HTML should contain the test name");
    }

    @Test
    public void buildHtml_containsClassName() {
        TestTiming t = timing("myTest");
        t.setTestClassName("LoginPageTest");
        String html = TraceRecorder.buildHtml(t, "myTest", null);
        Assert.assertTrue(html.contains("LoginPageTest"));
    }

    @Test
    public void buildHtml_containsErrorMessage() {
        TestTiming t = timing("failTest");
        t.setErrorMessage("Expected [Login] but found [Error 404]");
        String html = TraceRecorder.buildHtml(t, "failTest", null);
        Assert.assertTrue(html.contains("Expected [Login] but found [Error 404]"));
    }

    @Test
    public void buildHtml_containsStackTrace() {
        TestTiming t = timing("failTest");
        t.setStackTrace("org.testng.Assert$AssertionError\n  at LoginTest.testLogin(LoginTest.java:42)");
        String html = TraceRecorder.buildHtml(t, "failTest", null);
        Assert.assertTrue(html.contains("AssertionError"));
        Assert.assertTrue(html.contains("LoginTest.java:42"));
    }

    @Test
    public void buildHtml_containsFailureSection_onlyWhenErrorPresent() {
        TestTiming noError = timing("cleanTest");
        String htmlNoError = TraceRecorder.buildHtml(noError, "cleanTest", null);
        Assert.assertFalse(htmlNoError.contains("Failure Details"),
                "No error section when timing has no error");

        TestTiming withError = timing("badTest");
        withError.setErrorMessage("Boom");
        String htmlWithError = TraceRecorder.buildHtml(withError, "badTest", null);
        Assert.assertTrue(htmlWithError.contains("Failure Details"));
    }

    // ------------------------------------------------------------------
    // buildHtml — steps
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_noSteps_showsNoStepsMessage() {
        TestTiming t = timing("noStepTest");
        String html = TraceRecorder.buildHtml(t, "noStepTest", null);
        Assert.assertTrue(html.contains("No steps were logged"));
    }

    @Test
    public void buildHtml_withSteps_containsStepNames() {
        TestTiming t = timing("stepTest");
        t.addStep(new StepRecord("Navigate to login", 0L, "INFO", null));
        t.addStep(new StepRecord("Enter credentials", 500L, "INFO", null));
        t.addStep(new StepRecord("Click submit", 900L, "PASS", null));

        String html = TraceRecorder.buildHtml(t, "stepTest", null);
        Assert.assertTrue(html.contains("Navigate to login"));
        Assert.assertTrue(html.contains("Enter credentials"));
        Assert.assertTrue(html.contains("Click submit"));
    }

    @Test
    public void buildHtml_stepWithScreenshot_embedsClickHandler() {
        TestTiming t = timing("screenshotStep");
        t.addStep(new StepRecord("After login", 100L, "INFO", "abc123base64=="));

        String html = TraceRecorder.buildHtml(t, "screenshotStep", null);
        Assert.assertTrue(html.contains("showScreenshot(0)"),
                "Step with screenshot should have click handler");
        Assert.assertTrue(html.contains("abc123base64=="),
                "Base64 screenshot should be embedded in JS array");
    }

    @Test
    public void buildHtml_stepWithoutScreenshot_noClickHandler() {
        TestTiming t = timing("noScreenStep");
        t.addStep(new StepRecord("Plain step", 50L, "INFO", null));

        String html = TraceRecorder.buildHtml(t, "noScreenStep", null);
        // No screenshot means no onclick on the step div
        Assert.assertFalse(html.contains("showScreenshot(0)"));
    }

    // ------------------------------------------------------------------
    // buildHtml — final screenshot
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_finalScreenshot_embeddedAsImg() {
        TestTiming t = timing("failShot");
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAE=";
        String html = TraceRecorder.buildHtml(t, "failShot", base64);
        Assert.assertTrue(html.contains("data:image/png;base64," + base64),
                "Final screenshot should be embedded as data URI");
        Assert.assertTrue(html.contains("Final State Screenshot"));
    }

    @Test
    public void buildHtml_noFinalScreenshot_noFinalSection() {
        TestTiming t = timing("noShot");
        String html = TraceRecorder.buildHtml(t, "noShot", null);
        Assert.assertFalse(html.contains("Final State Screenshot"));
    }

    // ------------------------------------------------------------------
    // buildHtml — status badge
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_failedStatus_showsFailedBadge() {
        TestTiming t = timing("badTest");
        t.setStatus("FAILED");
        String html = TraceRecorder.buildHtml(t, "badTest", null);
        Assert.assertTrue(html.contains("status-failed"));
        Assert.assertTrue(html.contains("FAILED"));
    }

    // ------------------------------------------------------------------
    // HTML structure — self-contained
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_isValidHtmlSkeleton() {
        TestTiming t = timing("structTest");
        String html = TraceRecorder.buildHtml(t, "structTest", null);
        Assert.assertTrue(html.startsWith("<!DOCTYPE html>"));
        Assert.assertTrue(html.contains("<html"));
        Assert.assertTrue(html.contains("</html>"));
        Assert.assertTrue(html.contains("<style>"),    "CSS should be inlined");
        Assert.assertTrue(html.contains("<script>"),   "JS should be inlined");
        Assert.assertFalse(html.contains("cdn."),      "No CDN references");
        Assert.assertFalse(html.contains("https://"),  "No external URLs");
    }

    // ------------------------------------------------------------------
    // escHtml — XSS safety
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_escapesHtmlInTestName() {
        TestTiming t = timing("test<script>alert(1)</script>");
        String html = TraceRecorder.buildHtml(t, "test<script>alert(1)</script>", null);
        Assert.assertFalse(html.contains("<script>alert"),
                "Raw <script> should be escaped");
        Assert.assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    public void buildHtml_escapesHtmlInErrorMessage() {
        TestTiming t = timing("xssTest");
        t.setErrorMessage("<img src=x onerror=alert(1)>");
        String html = TraceRecorder.buildHtml(t, "xssTest", null);
        Assert.assertFalse(html.contains("<img src=x"),
                "Raw HTML in error should be escaped");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    @Test
    public void buildHtml_rendersConsoleErrors_escaped() {
        TestTiming t = timing("jsErrTest");
        t.setConsoleErrors(List.of("TypeError: x is undefined", "<script>alert(1)</script>"));
        String html = TraceRecorder.buildHtml(t, "jsErrTest", null);
        Assert.assertTrue(html.contains("Console Errors"), "console section expected");
        Assert.assertTrue(html.contains("TypeError: x is undefined"));
        Assert.assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "must be HTML-escaped");
        Assert.assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    public void buildHtml_omitsConsoleSection_whenNoErrors() {
        String html = TraceRecorder.buildHtml(timing("cleanJs"), "cleanJs", null);
        Assert.assertFalse(html.contains("Console Errors"));
    }

    @Test
    public void buildHtml_groupsLocatorsUnderTheStepTheyRanIn() {
        TestTiming t = timing("locTest");
        t.addStep(new StepRecord("Open login", 0, "INFO", null));
        t.addStep(new StepRecord("Submit form", 500, "INFO", null));
        t.addLocatorUse(new LocatorUse("getByLabel(\"User\")", 100, true));
        t.addLocatorUse(new LocatorUse("getByRole(BUTTON).withName(\"Go\")", 600, false));
        String html = TraceRecorder.buildHtml(t, "locTest", null);
        int open = html.indexOf("Locators Used");
        Assert.assertTrue(open > 0, "locators section expected");
        String section = html.substring(open);
        int s1 = section.indexOf("Open login"), l1 = section.indexOf("getByLabel(&quot;User&quot;)");
        int s2 = section.indexOf("Submit form"), l2 = section.indexOf("getByRole(BUTTON).withName(&quot;Go&quot;)");
        Assert.assertTrue(s1 >= 0 && s1 < l1 && l1 < s2 && s2 < l2, section);
        Assert.assertTrue(section.contains("locator-fail"), "failed lookup must be marked");
    }

    @Test
    public void buildHtml_locatorBeforeFirstStep_isListedUnderPreamble() {
        TestTiming t = timing("preTest");
        t.addStep(new StepRecord("Later step", 400, "INFO", null));
        t.addLocatorUse(new LocatorUse("css(#a)", 10, true));
        String html = TraceRecorder.buildHtml(t, "preTest", null);
        Assert.assertTrue(html.contains("Before first step"));
    }

    @Test
    public void buildHtml_omitsLocatorSection_whenNoneRecorded() {
        Assert.assertFalse(TraceRecorder.buildHtml(timing("noLoc"), "noLoc", null).contains("Locators Used"));
    }

    @Test
    public void buildHtml_groupsConsoleEntriesUnderTheStepTheyHappenedIn() {
        TestTiming t = timing("consoleStepTest");
        t.addStep(new StepRecord("Open page", 0, "INFO", null));
        t.addStep(new StepRecord("Click save", 500, "INFO", null));
        t.setConsoleErrors(List.of("early error", "late error"));
        t.addConsoleEntry(new ConsoleEntry("early error", 100));
        t.addConsoleEntry(new ConsoleEntry("late error", 700));
        String section = sectionFrom(TraceRecorder.buildHtml(t, "consoleStepTest", null), "Console Errors");
        int s1 = section.indexOf("Open page"), e1 = section.indexOf("early error");
        int s2 = section.indexOf("Click save"), e2 = section.indexOf("late error");
        Assert.assertTrue(s1 >= 0 && s1 < e1 && e1 < s2 && s2 < e2, section);
        Assert.assertEquals(section.split("early error", -1).length, 2, "must not be listed twice");
    }

    @Test
    public void buildHtml_consoleErrorsWithoutTimestamps_stillListedFlat() {
        TestTiming t = timing("shimOnly");
        t.setConsoleErrors(List.of("shim error"));
        String html = TraceRecorder.buildHtml(t, "shimOnly", null);
        Assert.assertTrue(html.contains("shim error"));
    }

    @Test
    public void buildHtml_groupsNetworkFailuresUnderStep_andEscapes() {
        TestTiming t = timing("netTest");
        t.addStep(new StepRecord("Submit order", 0, "INFO", null));
        t.addNetworkFailure(new NetworkFailure("POST", "http://x/api/orders?a=<b>", 500, null, 250));
        t.addNetworkFailure(new NetworkFailure("GET", "http://x/img.png", 0, "net::ERR_CONNECTION_REFUSED", 300));
        String html = TraceRecorder.buildHtml(t, "netTest", null);
        String section = sectionFrom(html, "Failed Requests");
        Assert.assertTrue(section.contains("Submit order"), section);
        Assert.assertTrue(section.contains("POST") && section.contains("500"), section);
        Assert.assertTrue(section.contains("net::ERR_CONNECTION_REFUSED"), section);
        Assert.assertTrue(section.contains("a=&lt;b&gt;"), "url must be escaped");
    }

    @Test
    public void buildHtml_omitsNetworkSection_whenNone() {
        Assert.assertFalse(TraceRecorder.buildHtml(timing("noNet"), "noNet", null).contains("Failed Requests"));
    }

    private static String sectionFrom(String html, String title) {
        int i = html.indexOf(">" + title);
        Assert.assertTrue(i > 0, title + " section expected");
        return html.substring(i);
    }

    private static TestTiming timing(String testName) {
        TestTiming t = new TestTiming(testName, "test-thread");
        t.setStatus("FAILED");
        return t;
    }
}
