package com.seleniumboot.unit;

import com.seleniumboot.hooks.ExecutionHook;
import com.seleniumboot.hooks.HookRegistry;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link HookRegistry}.
 */
public class HookRegistryTest {

    @AfterMethod
    public void resetRegistry() throws Exception {
        Field f = HookRegistry.class.getDeclaredField("hooks");
        f.setAccessible(true);
        ((List<?>) f.get(null)).clear();
    }

    @Test
    public void onSuiteStart_firesRegisteredHook() {
        TrackingHook hook = new TrackingHook();
        HookRegistry.register(hook);
        HookRegistry.onSuiteStart();

        assertTrue(hook.suiteStarted);
    }

    @Test
    public void onSuiteEnd_firesRegisteredHook() {
        TrackingHook hook = new TrackingHook();
        HookRegistry.register(hook);
        HookRegistry.onSuiteEnd();

        assertTrue(hook.suiteEnded);
    }

    @Test
    public void onTestStart_passesTestIdToHook() {
        TrackingHook hook = new TrackingHook();
        HookRegistry.register(hook);
        HookRegistry.onTestStart("com.example.FooTest#bar");

        assertEquals(hook.lastTestId, "com.example.FooTest#bar");
        assertTrue(hook.testStarted);
    }

    @Test
    public void onTestEnd_passesTestIdAndStatusToHook() {
        TrackingHook hook = new TrackingHook();
        HookRegistry.register(hook);
        HookRegistry.onTestEnd("com.example.FooTest#bar", "PASSED");

        assertEquals(hook.lastTestId, "com.example.FooTest#bar");
        assertEquals(hook.lastStatus, "PASSED");
    }

    @Test
    public void onTestFailure_passesTestIdAndCauseToHook() {
        TrackingHook hook = new TrackingHook();
        HookRegistry.register(hook);
        RuntimeException cause = new RuntimeException("boom");
        HookRegistry.onTestFailure("com.example.FooTest#bar", cause);

        assertEquals(hook.lastFailureCause, cause);
    }

    @Test
    public void hookException_doesNotAbortOtherHooks() {
        HookRegistry.register(new BrokenHook());
        TrackingHook good = new TrackingHook();
        HookRegistry.register(good);

        HookRegistry.onSuiteStart(); // BrokenHook throws, but good hook should still fire

        assertTrue(good.suiteStarted, "Second hook should still fire after first throws");
    }

    @Test
    public void multipleHooks_allReceiveEvent() {
        TrackingHook h1 = new TrackingHook();
        TrackingHook h2 = new TrackingHook();
        HookRegistry.register(h1);
        HookRegistry.register(h2);
        HookRegistry.onSuiteStart();

        assertTrue(h1.suiteStarted);
        assertTrue(h2.suiteStarted);
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    static class TrackingHook implements ExecutionHook {
        boolean suiteStarted;
        boolean suiteEnded;
        boolean testStarted;
        String lastTestId;
        String lastStatus;
        Throwable lastFailureCause;

        @Override public void onSuiteStart() { suiteStarted = true; }
        @Override public void onSuiteEnd() { suiteEnded = true; }
        @Override public void onTestStart(String testId) { testStarted = true; lastTestId = testId; }
        @Override public void onTestEnd(String testId, String status) { lastTestId = testId; lastStatus = status; }
        @Override public void onTestFailure(String testId, Throwable cause) { lastTestId = testId; lastFailureCause = cause; }
    }

    static class BrokenHook implements ExecutionHook {
        @Override public void onSuiteStart() { throw new RuntimeException("simulated hook failure"); }
    }
}
