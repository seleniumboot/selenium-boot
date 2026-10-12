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
        Assert.assertEquals(FailureClassifier.classify(null, null).category(), FailureCategory.UNKNOWN);
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
}
