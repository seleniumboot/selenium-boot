package com.seleniumboot.precondition;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.lang.reflect.Method;

/**
 * Executes {@link PreCondition} setup for a test method.
 *
 * <p>For each condition name declared on the test:
 * <ol>
 *   <li>If a valid cached session exists for this thread → restore cookies + localStorage.</li>
 *   <li>Otherwise → find the {@link ConditionProvider} method, run it, cache the session.</li>
 * </ol>
 *
 * <p>On retry, the cache is invalidated so the provider runs fresh.
 */
public final class PreConditionRunner {

    private PreConditionRunner() {}

    /**
     * Evaluates all {@link PreCondition} annotations on the test method and satisfies them.
     * No-op if the test has no {@code @PreCondition}.
     *
     * @param result the TestNG test result (used to read annotations and retry count)
     */
    public static void run(ITestResult result) {
        Method testMethod = result.getMethod().getConstructorOrMethod().getMethod();
        PreCondition annotation = testMethod.getAnnotation(PreCondition.class);
        if (annotation == null) return;

        boolean isRetry = result.getMethod().getCurrentInvocationCount() > 0;
        WebDriver driver = DriverManager.getDriver();

        for (String conditionName : annotation.value()) {
            if (isRetry) {
                SessionCache.invalidate(conditionName);
            }

            if (SessionCache.isValid(conditionName)) {
                restoreSession(conditionName, driver);
            } else {
                runProvider(conditionName, driver);
            }
        }
    }

    /**
     * JUnit 5 / framework-agnostic entry point.
     *
     * <p>Looks for {@link PreCondition} on the test method, then on the declaring class.
     * Runs or restores the session for each condition name.
     *
     * @param testMethod the test method about to execute
     * @param isRetry    true when this is a retry attempt — invalidates the cached session
     *                   so the provider runs fresh rather than restoring a potentially
     *                   broken session from the failed attempt
     */
    public static void run(Method testMethod, boolean isRetry) {
        PreCondition annotation = testMethod.getAnnotation(PreCondition.class);
        if (annotation == null) {
            annotation = testMethod.getDeclaringClass().getAnnotation(PreCondition.class);
        }
        if (annotation == null) return;

        WebDriver driver = DriverManager.getDriver();
        for (String conditionName : annotation.value()) {
            if (isRetry) SessionCache.invalidate(conditionName);
            if (SessionCache.isValid(conditionName)) {
                restoreSession(conditionName, driver);
            } else {
                runProvider(conditionName, driver);
            }
        }
    }

    /**
     * Clears all session caches for the current thread.
     * Called at suite end to release memory.
     */
    public static void clearAll() {
        SessionCache.clearAll();
    }

    private static void restoreSession(String conditionName, WebDriver driver) {
        System.out.println("[PreCondition] Restoring cached session for: " + conditionName);
        SessionCache.restore(conditionName, driver);
    }

    private static void runProvider(String conditionName, WebDriver driver) {
        PreConditionRegistry.ProviderMethod pm = PreConditionRegistry.find(conditionName);
        if (pm == null) {
            throw new IllegalStateException(
                "[PreCondition] No @ConditionProvider found for condition: '" + conditionName + "'. " +
                "Register a BaseConditions subclass with a method annotated " +
                "@ConditionProvider(\"" + conditionName + "\")."
            );
        }

        System.out.println("[PreCondition] Running provider for: " + conditionName);
        try {
            pm.invoke();
            SessionCache.store(conditionName, driver);
            System.out.println("[PreCondition] Session cached for: " + conditionName);
        } catch (Exception e) {
            // InvocationTargetException wraps the real cause — unwrap it for a useful message
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            throw new RuntimeException(
                "[PreCondition] Provider failed for condition '" + conditionName + "': " + cause.getMessage(), cause
            );
        }
    }
}
