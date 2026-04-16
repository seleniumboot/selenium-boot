package com.seleniumboot.unit;

import com.seleniumboot.metrics.TestTiming;
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

    private static TestTiming timing(String testName) {
        TestTiming t = new TestTiming(testName, "test-thread");
        t.setStatus("FAILED");
        return t;
    }
}
