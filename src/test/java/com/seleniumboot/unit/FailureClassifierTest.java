package com.seleniumboot.unit;

import com.seleniumboot.failure.FailureCategory;
import com.seleniumboot.failure.FailureClassifier;
import com.seleniumboot.metrics.TestTiming;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Pure-function tests for {@link FailureClassifier}; no browser or I/O. */
public class FailureClassifierTest {

    private static FailureCategory classify(Throwable t) {
        return result(t).category();
    }

    private static FailureClassifier.Result result(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        TestTiming timing = new TestTiming("T.t", "main");
        timing.setErrorMessage(t.getMessage());
        timing.setStackTrace(sw.toString());
        return FailureClassifier.classify(timing);
    }

    @Test
    public void locatorException_withTimeoutCause_isLocatorNotFound() {
        Throwable t = new com.seleniumboot.locator.LocatorException("No element for css=#x",
                new org.openqa.selenium.TimeoutException("Expected condition failed"));
        Assert.assertEquals(classify(t), FailureCategory.LOCATOR_NOT_FOUND);
    }

    @Test
    public void bareTimeoutException_isTimeout() {
        Assert.assertEquals(classify(new org.openqa.selenium.TimeoutException("waiting for visibility")),
                FailureCategory.TIMEOUT);
    }

    @Test
    public void timeoutFromPageLoad_isNavigationError() {
        Assert.assertEquals(classify(new org.openqa.selenium.TimeoutException(
                "timeout: Timed out receiving message from renderer: 300.000")), FailureCategory.NAVIGATION_ERROR);
    }

    @Test
    public void dnsError_isNavigationError() {
        Assert.assertEquals(classify(new org.openqa.selenium.WebDriverException(
                "unknown error: net::ERR_NAME_NOT_RESOLVED")), FailureCategory.NAVIGATION_ERROR);
    }

    @Test
    public void http5xx_isNavigationError() {
        Assert.assertEquals(classify(new RuntimeException("Unexpected HTTP 503 from /health")),
                FailureCategory.NAVIGATION_ERROR);
    }

    @Test
    public void noSuchElement_isLocatorNotFound() {
        Assert.assertEquals(classify(new org.openqa.selenium.NoSuchElementException("no such element")),
                FailureCategory.LOCATOR_NOT_FOUND);
    }

    @Test
    public void missingElement_withCapabilitiesDump_isLocatorNotFoundNotNavigationError() {
        String msg = "no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#does-not-exist\"}\n"
                + "  (Session info: chrome=130.0.6723.70)\n"
                + "Capabilities {acceptInsecureCerts: false, browserName: chrome, browserVersion: 130.0.6723.70, pageLoadStrategy: normal, platformName: windows}";
        String trace = "org.openqa.selenium.NoSuchElementException: " + msg + "\n"
                + "\tat org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.createException(W3CHttpResponseCodec.java:200)\n"
                + "\tat example.SearchTest.testMissingElement(SearchTest.java:42)";

        FailureClassifier.Result r = FailureClassifier.classify(msg, trace);
        Assert.assertEquals(r.category(), FailureCategory.LOCATOR_NOT_FOUND,
                "NoSuchElementException must not be classified as Navigation error due to pageLoadStrategy in capabilities");
        Assert.assertEquals(r.type(), com.seleniumboot.failure.FailureType.TEST_BUG);
    }

    @Test
    public void staleElement_isStale() {
        Assert.assertEquals(classify(new org.openqa.selenium.StaleElementReferenceException("stale")),
                FailureCategory.STALE_ELEMENT);
    }

    @Test
    public void staleElement_nestedInCause_isStale() {
        Throwable t = new RuntimeException("click failed",
                new org.openqa.selenium.StaleElementReferenceException("stale"));
        Assert.assertEquals(classify(t), FailureCategory.STALE_ELEMENT);
    }

    @Test
    public void elementNotInteractable_isNotInteractable() {
        Assert.assertEquals(classify(new org.openqa.selenium.ElementNotInteractableException("hidden")),
                FailureCategory.ELEMENT_NOT_INTERACTABLE);
    }

    @Test
    public void clickIntercepted_isNotInteractable() {
        Assert.assertEquals(classify(new org.openqa.selenium.ElementClickInterceptedException("covered")),
                FailureCategory.ELEMENT_NOT_INTERACTABLE);
    }

    @Test
    public void assertionError_isAssertionMismatch() {
        Assert.assertEquals(classify(new AssertionError("expected [a] but found [b]")),
                FailureCategory.ASSERTION_MISMATCH);
    }

    @Test
    public void assertionError_mentioningTimeout_staysAssertionMismatch() {
        Assert.assertEquals(classify(new AssertionError("expected \"Timed out\" but found \"OK\"")),
                FailureCategory.ASSERTION_MISMATCH);
    }

    @Test
    public void noSuchSession_isBrowserCrash() {
        Assert.assertEquals(classify(new org.openqa.selenium.NoSuchSessionException("invalid session id")),
                FailureCategory.BROWSER_CRASH);
    }

    @Test
    public void pageCrash_isBrowserCrash() {
        Assert.assertEquals(classify(new org.openqa.selenium.WebDriverException(
                "unknown error: session deleted because of page crash")), FailureCategory.BROWSER_CRASH);
    }

    @Test
    public void unrecognisedError_isUnknown() {
        Assert.assertEquals(classify(new IllegalStateException("something odd")), FailureCategory.UNKNOWN);
    }

    @Test
    public void noData_isUnknown() {
        Assert.assertEquals(FailureClassifier.classify((TestTiming) null).category(), FailureCategory.UNKNOWN);
        Assert.assertEquals(FailureClassifier.classify((String) null, (String) null).category(), FailureCategory.UNKNOWN);
    }

    @Test
    public void evidence_namesTheMatchedClass() {
        Assert.assertTrue(result(new org.openqa.selenium.StaleElementReferenceException("stale"))
                .evidence().contains("StaleElementReferenceException"));
    }

    @Test
    public void multilineSeleniumMessage_isNotMistakenForCause() {
        Throwable t = new org.openqa.selenium.WebDriverException(
                "boom\nCommand: [abc, get {url=x}]\nBuild info: version: '4'");
        Assert.assertEquals(classify(t), FailureCategory.UNKNOWN);
    }

    @Test
    public void failureType_mapsCorrectlyAcrossCategories() {
        Assert.assertEquals(result(new AssertionError("expected a but got b")).type(),
                com.seleniumboot.failure.FailureType.PRODUCT_BUG);
        Assert.assertEquals(result(new org.openqa.selenium.NoSuchElementException("missing")).type(),
                com.seleniumboot.failure.FailureType.TEST_BUG);
        Assert.assertEquals(result(new org.openqa.selenium.ElementNotInteractableException("hidden")).type(),
                com.seleniumboot.failure.FailureType.TEST_BUG);
        Assert.assertEquals(result(new org.openqa.selenium.StaleElementReferenceException("stale")).type(),
                com.seleniumboot.failure.FailureType.TEST_BUG);
        Assert.assertEquals(result(new org.openqa.selenium.NoSuchSessionException("crash")).type(),
                com.seleniumboot.failure.FailureType.ENVIRONMENT);
        Assert.assertEquals(result(new RuntimeException("net::ERR_NAME_NOT_RESOLVED")).type(),
                com.seleniumboot.failure.FailureType.ENVIRONMENT);
        Assert.assertEquals(result(new org.openqa.selenium.TimeoutException("timeout")).type(),
                com.seleniumboot.failure.FailureType.UNKNOWN);
        Assert.assertEquals(result(new RuntimeException("weird unrecognised error")).type(),
                com.seleniumboot.failure.FailureType.UNKNOWN);
    }

    @Test
    public void failureType_retryOrIntermittentStatus_isFlaky() {
        FailureClassifier.Result r1 = FailureClassifier.classify("assertion failed", "AssertionError: boom",
                com.seleniumboot.flakiness.TestStatus.PASSED_ON_RETRY);
        Assert.assertEquals(r1.type(), com.seleniumboot.failure.FailureType.FLAKY);
        Assert.assertEquals(r1.displayLabel(), "Flaky (Assertion mismatch)");
        Assert.assertTrue(r1.suggestedAction().contains("retry"));

        FailureClassifier.Result r2 = FailureClassifier.classify("timeout", "TimeoutException: boom",
                com.seleniumboot.flakiness.TestStatus.INTERMITTENT);
        Assert.assertEquals(r2.type(), com.seleniumboot.failure.FailureType.FLAKY);
        Assert.assertEquals(r2.displayLabel(), "Flaky (Timeout)");
    }

    @Test
    public void failureType_newlyFailingAssertion_isProductBugWithRegressionNote() {
        FailureClassifier.Result r = FailureClassifier.classify("expected 10 but found 5",
                "java.lang.AssertionError: expected 10 but found 5",
                com.seleniumboot.flakiness.TestStatus.NEWLY_FAILING);
        Assert.assertEquals(r.type(), com.seleniumboot.failure.FailureType.PRODUCT_BUG);
        Assert.assertTrue(r.suggestedAction().contains("regressed") || r.suggestedAction().contains("expectation"),
                r.suggestedAction());
    }

    @Test
    public void suggestedAction_enrichedWithFailingLocator() {
        TestTiming timing = new TestTiming("com.example.CartTest.checkout", "main");
        timing.setFailingLocator("By.cssSelector: #checkout-button");
        timing.setErrorMessage("Element not found By.cssSelector: #checkout-button");
        timing.setStackTrace("org.openqa.selenium.NoSuchElementException: not found\n\tat example.Cart.pay(Cart.java:55)");

        FailureClassifier.Result result = FailureClassifier.classify(timing);
        Assert.assertEquals(result.category(), FailureCategory.LOCATOR_NOT_FOUND);
        Assert.assertEquals(result.type(), com.seleniumboot.failure.FailureType.TEST_BUG);
        Assert.assertTrue(result.suggestedAction().contains("#checkout-button"), result.suggestedAction());
    }

    @Test
    public void suggestedAction_http5xx_givesBackendGuidance() {
        FailureClassifier.Result r = FailureClassifier.classify("HTTP 503 Service Unavailable", "status: 503");
        Assert.assertEquals(r.category(), FailureCategory.NAVIGATION_ERROR);
        Assert.assertTrue(r.suggestedAction().contains("HTTP 5xx"), r.suggestedAction());
    }

    @Test
    public void result_backwardCompatibleConstructors() {
        FailureClassifier.Result r2 = new FailureClassifier.Result(FailureCategory.LOCATOR_NOT_FOUND, "evidence");
        Assert.assertEquals(r2.category(), FailureCategory.LOCATOR_NOT_FOUND);
        Assert.assertEquals(r2.type(), com.seleniumboot.failure.FailureType.TEST_BUG);
        Assert.assertNotNull(r2.suggestedAction());

        FailureClassifier.Result r3 = new FailureClassifier.Result(FailureCategory.LOCATOR_NOT_FOUND,
                com.seleniumboot.failure.FailureType.FLAKY, "evidence");
        Assert.assertEquals(r3.type(), com.seleniumboot.failure.FailureType.FLAKY);
        Assert.assertEquals(r3.displayLabel(), "Flaky (Locator not found)");
    }

    /** SonarCloud java:S5998: a very long dotted line must not blow the regex stack. */
    @Test
    public void veryLongDottedLine_doesNotOverflowStack() {
        String longLine = "a.".repeat(50_000) + "Boom";
        FailureClassifier.Result r = FailureClassifier.classify("boom", longLine + "\n\tat x.y(Z.java:1)");
        Assert.assertNotNull(r.category());
    }
}
