package com.seleniumboot.unit;

import com.seleniumboot.hooks.ExecutionHook;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.junit5.SeleniumBootLauncherListener;
import com.seleniumboot.precondition.PreConditionRegistry;
import com.seleniumboot.testmanagement.TestManagementReporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumBootLauncherListener} — the JUnit Platform
 * counterpart of {@code SuiteExecutionListener}.
 */
public class SeleniumBootLauncherListenerTest {

    @AfterMethod
    public void resetRegistry() throws Exception {
        Field f = HookRegistry.class.getDeclaredField("hooks");
        f.setAccessible(true);
        ((List<?>) f.get(null)).clear();

        Field suiteStartedField = SeleniumBootLauncherListener.class.getDeclaredField("suiteStarted");
        suiteStartedField.setAccessible(true);
        ((AtomicBoolean) suiteStartedField.get(null)).set(false);

        TestManagementReporter.getInstance().onSuiteEnd();
    }

    @Test
    public void testPlanExecutionStarted_firesSuiteStartHookOnce() {
        CountingHook hook = new CountingHook();
        HookRegistry.register(hook);

        new SeleniumBootLauncherListener().testPlanExecutionStarted(null);

        assertEquals(hook.suiteStarts, 1, "onSuiteStart must fire once per test plan");
    }

    @Test
    public void testPlanExecutionStarted_isIdempotentOnRepeatedCalls() {
        CountingHook hook = new CountingHook();
        HookRegistry.register(hook);

        SeleniumBootLauncherListener listener = new SeleniumBootLauncherListener();
        listener.testPlanExecutionStarted(null);
        listener.testPlanExecutionStarted(null);
        listener.testPlanExecutionStarted(null);

        assertEquals(hook.suiteStarts, 1, "onSuiteStart must not double-fire when testPlanExecutionStarted is called repeatedly");
    }

    @Test
    public void testPlanExecutionFinished_resetsGuardForNextRun() {
        CountingHook hook = new CountingHook();
        HookRegistry.register(hook);

        SeleniumBootLauncherListener listener = new SeleniumBootLauncherListener();
        listener.testPlanExecutionStarted(null);
        assertEquals(hook.suiteStarts, 1);

        listener.testPlanExecutionFinished(null);

        // Next run should fire onSuiteStart again
        listener.testPlanExecutionStarted(null);
        assertEquals(hook.suiteStarts, 2, "After testPlanExecutionFinished, a new test plan should fire onSuiteStart again");
    }

    @Test
    public void testPlanExecutionStarted_loadsPreConditions() {
        new SeleniumBootLauncherListener().testPlanExecutionStarted(null);

        // PreConditionRegistry.loadAll() is safe and idempotent
        assertNotNull(PreConditionRegistry.getProviders());
    }

    private static final class CountingHook implements ExecutionHook {
        int suiteStarts;

        @Override
        public void onSuiteStart() {
            suiteStarts++;
        }
    }
}
