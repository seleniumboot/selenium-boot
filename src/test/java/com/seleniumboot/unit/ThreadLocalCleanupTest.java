package com.seleniumboot.unit;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.listeners.TestExecutionListener;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.test.BasePage;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import org.testng.IClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Covers the ThreadLocal clean-up paths (SonarCloud java:S5164): values are removed,
 * not reset, and behaviour stays the same.
 */
public class ThreadLocalCleanupTest {

    // ── BasePage frame depth ─────────────────────────────────────────

    static class FramePage extends BasePage {
        FramePage(WebDriver d) { super(d); }
        void frame(int i, Runnable r) { withinFrameIndex(i, r); }
    }

    @Test
    public void nestedFrames_restoreParentThenDefault_andCanRepeat() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.TargetLocator to = mock(WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(to);
        FramePage page = new FramePage(driver);

        for (int round = 0; round < 2; round++) {   // second round runs after FRAME_DEPTH.remove()
            page.frame(0, () -> page.frame(1, () -> {}));
        }

        InOrder order = inOrder(to);
        for (int round = 0; round < 2; round++) {
            order.verify(to).frame(0);
            order.verify(to).frame(1);
            order.verify(to).parentFrame();
            order.verify(to).defaultContent();
        }
    }

    // ── TestExecutionListener jsErrorsLogged ─────────────────────────

    @BeforeMethod
    @AfterMethod
    public void reset() {
        ExecutionMetrics.reset();
        try { DriverManager.quitDriver(); } catch (Exception ignored) {}
    }

    @Test
    public void listener_successThenFailure_recordStatuses() {
        TestExecutionListener listener = new TestExecutionListener();

        ITestResult pass = noBrowserResult("com.example.T.passes");
        listener.onTestStart(pass);
        listener.onTestSuccess(pass);
        assertEquals(ExecutionMetrics.getTiming("com.example.T.passes").getStatus(), "PASSED");

        ITestResult fail = noBrowserResult("com.example.T.fails");
        when(fail.getThrowable()).thenReturn(new AssertionError("boom"));
        listener.onTestStart(fail);
        listener.onTestFailure(fail);
        assertEquals(ExecutionMetrics.getTiming("com.example.T.fails").getStatus(), "FAILED");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ITestResult noBrowserResult(String qualifiedName) {
        ITestResult result = mock(ITestResult.class);
        ITestNGMethod method = mock(ITestNGMethod.class);
        IClass testClass = mock(IClass.class);
        org.testng.ITestContext context = mock(org.testng.ITestContext.class);
        org.testng.xml.XmlTest xmlTest = mock(org.testng.xml.XmlTest.class);
        when(result.getMethod()).thenReturn(method);
        when(result.getTestClass()).thenReturn(testClass);
        when(result.getTestContext()).thenReturn(context);
        when(context.getCurrentXmlTest()).thenReturn(xmlTest);
        when(method.getQualifiedName()).thenReturn(qualifiedName);
        when(method.getMethodName()).thenReturn(qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1));
        when(method.getGroups()).thenReturn(new String[0]);
        try {
            when(method.getConstructorOrMethod()).thenReturn(
                    new org.testng.internal.ConstructorOrMethod(NoBrowserTests.class.getDeclaredMethod("t")));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        when(testClass.getRealClass()).thenReturn((Class) NoBrowserTests.class);
        return result;
    }

    static class NoBrowserTests {
        @com.seleniumboot.test.NoBrowser
        void t() {}
    }
}
