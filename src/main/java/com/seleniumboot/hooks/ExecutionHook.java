package com.seleniumboot.hooks;

/**
 * Lifecycle hook for Selenium Boot execution events.
 *
 * <p>All methods have default no-op implementations — implement only the
 * events you care about.
 *
 * <p>Register via Java SPI:
 * <pre>META-INF/services/com.seleniumboot.hooks.ExecutionHook</pre>
 * or programmatically before framework boot:
 * <pre>HookRegistry.register(new MyHook());</pre>
 *
 * <p>Example — timing every test to an external system:
 * <pre>
 * public class TimingHook implements ExecutionHook {
 *     public void onTestEnd(String testId, String status) {
 *         metricsClient.record(testId, status);
 *     }
 * }
 * </pre>
 */
public interface ExecutionHook {

    /** Called once when the TestNG suite starts, after framework bootstrap. */
    default void onSuiteStart() {}

    /** Called once when the TestNG suite finishes, after all reports are generated. */
    default void onSuiteEnd() {}

    /**
     * Called at the start of each test method, after the WebDriver is created.
     *
     * @param testId fully-qualified test method name (e.g. {@code com.example.LoginTest#login})
     */
    default void onTestStart(String testId) {}

    /**
     * Called at the end of each test method (success or skip), after the driver is quit.
     *
     * @param testId fully-qualified test method name
     * @param status {@code "PASSED"} or {@code "SKIPPED"}
     */
    default void onTestEnd(String testId, String status) {}

    /**
     * Called when a test method fails, after the screenshot is captured and
     * before the driver is quit.
     *
     * @param testId fully-qualified test method name
     * @param cause  the throwable that caused the failure
     */
    default void onTestFailure(String testId, Throwable cause) {}
}
