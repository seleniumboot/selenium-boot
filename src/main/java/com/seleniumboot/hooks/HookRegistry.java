package com.seleniumboot.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads and dispatches {@link ExecutionHook} lifecycle events.
 *
 * <p>Hook failures are isolated — an exception in one hook is logged and
 * does not prevent other hooks or framework operations from running.
 */
public final class HookRegistry {

    private static final List<ExecutionHook> hooks = new ArrayList<>();

    private HookRegistry() {}

    /**
     * Discovers all SPI-registered hooks. Safe to call multiple times.
     */
    public static synchronized void loadAll() {
        ServiceLoader<ExecutionHook> loader = ServiceLoader.load(ExecutionHook.class);
        for (ExecutionHook hook : loader) {
            hooks.add(hook);
            System.out.println(
                "[Selenium Boot] ExecutionHook loaded: " + hook.getClass().getSimpleName()
            );
        }
    }

    /** Programmatically registers a hook. */
    public static synchronized void register(ExecutionHook hook) {
        hooks.add(hook);
    }

    public static void onSuiteStart() {
        for (ExecutionHook h : hooks) {
            try { h.onSuiteStart(); } catch (Exception e) { warn("onSuiteStart", h, e); }
        }
    }

    public static void onSuiteEnd() {
        for (ExecutionHook h : hooks) {
            try { h.onSuiteEnd(); } catch (Exception e) { warn("onSuiteEnd", h, e); }
        }
    }

    public static void onTestStart(String testId) {
        for (ExecutionHook h : hooks) {
            try { h.onTestStart(testId); } catch (Exception e) { warn("onTestStart", h, e); }
        }
    }

    public static void onTestEnd(String testId, String status) {
        for (ExecutionHook h : hooks) {
            try { h.onTestEnd(testId, status); } catch (Exception e) { warn("onTestEnd", h, e); }
        }
    }

    public static void onTestFailure(String testId, Throwable cause) {
        for (ExecutionHook h : hooks) {
            try { h.onTestFailure(testId, cause); } catch (Exception e) { warn("onTestFailure", h, e); }
        }
    }

    private static void warn(String event, ExecutionHook hook, Exception e) {
        System.err.println(
            "[Selenium Boot] Hook error [" + hook.getClass().getSimpleName()
            + "#" + event + "]: " + e.getMessage()
        );
    }
}
