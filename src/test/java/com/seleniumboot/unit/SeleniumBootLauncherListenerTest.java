package com.seleniumboot.unit;

import com.seleniumboot.hooks.ExecutionHook;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.junit5.SeleniumBootLauncherListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertEquals;

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
    }

    @Test
    public void testPlanExecutionStarted_firesSuiteStartHookOnce() {
        CountingHook hook = new CountingHook();
        HookRegistry.register(hook);

        new SeleniumBootLauncherListener().testPlanExecutionStarted(null);

        assertEquals(hook.suiteStarts, 1, "onSuiteStart must fire once per test plan");
    }

    private static final class CountingHook implements ExecutionHook {
        int suiteStarts;

        @Override
        public void onSuiteStart() {
            suiteStarts++;
        }
    }
}
