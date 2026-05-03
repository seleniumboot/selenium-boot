package com.seleniumboot.session;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.driver.DriverProviderFactory;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages named WebDriver sessions within a single test — enabling multi-browser
 * coordination such as admin/user flows, chat apps, or approval workflows.
 *
 * <p>Usage via {@code BaseTest} or {@code BaseJUnit5Test}:
 * <pre>
 * withSession("admin", () -&gt; {
 *     open("/admin/approvals");
 *     $(By.id("approve-btn")).click();
 * });
 * withSession("user", () -&gt; {
 *     open("/dashboard");
 *     assertThat(By.id("status")).hasText("Approved");
 * });
 * </pre>
 *
 * <p>All named sessions are automatically closed at test end by the framework.
 * Nesting {@code withSession()} calls is supported — the correct driver is
 * restored on each exit.
 */
@SeleniumBootApi(since = "1.12.0")
public final class MultiSessionManager {

    /**
     * Functional interface for session-scoped actions.
     * Accepts checked exceptions so test lambdas need not wrap them.
     */
    @FunctionalInterface
    public interface SessionAction {
        void run() throws Exception;
    }

    private static final ThreadLocal<Map<String, WebDriver>> NAMED =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private MultiSessionManager() {}

    /**
     * Returns the named session's {@link WebDriver}, creating a new browser instance
     * on first access. Subsequent calls with the same name return the same driver.
     */
    public static WebDriver getSession(String name) {
        return NAMED.get().computeIfAbsent(name, k -> createNewDriver(k));
    }

    /**
     * Switches the current thread's active driver to the named session, runs the
     * action, then restores the previous driver — even if the action throws.
     * Supports nested calls (stack-based restoration).
     */
    public static void withSession(String name, SessionAction action) {
        WebDriver sessionDriver = getSession(name);
        DriverManager.pushSessionOverride(sessionDriver);
        try {
            action.run();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[MultiSession] Action in session '" + name + "' threw: " + e.getMessage(), e);
        } finally {
            DriverManager.popSessionOverride();
        }
    }

    /**
     * Quits all named-session drivers for the current thread and clears the registry.
     * Called automatically by the framework at the end of each test.
     */
    public static void clearAll() {
        Map<String, WebDriver> sessions = NAMED.get();
        for (Map.Entry<String, WebDriver> entry : sessions.entrySet()) {
            try {
                entry.getValue().quit();
            } catch (Exception ignored) {}
        }
        sessions.clear();
        NAMED.remove();
    }

    private static WebDriver createNewDriver(String name) {
        try {
            WebDriver driver = DriverProviderFactory.getProvider().createDriver();
            System.out.println("[MultiSession] Created driver for session: " + name);
            return driver;
        } catch (Exception e) {
            throw new IllegalStateException(
                "[MultiSession] Failed to create driver for session '" + name + "': " + e.getMessage(), e);
        }
    }
}
