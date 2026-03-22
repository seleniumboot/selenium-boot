package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global session cache for authenticated session reuse across tests.
 *
 * <p>Stores cookies and localStorage under a named key so that expensive login
 * flows only run once per suite, regardless of how many tests need that session.
 *
 * <p>Unlike {@code @PreCondition} (which is thread-local and per-condition),
 * {@code SessionCache} is shared across all threads — any thread can store a session
 * and any other thread can restore it into its own driver.
 *
 * <p>Usage:
 * <pre>
 * // In a @BeforeSuite or the first test that needs login:
 * new LoginPage(getDriver()).login("admin", "secret");
 * SessionCache.store("adminSession");
 *
 * // In every other test that needs the authenticated state:
 * open("/");
 * SessionCache.restore("adminSession");
 * // Driver is now authenticated — no login page needed
 * </pre>
 *
 * <p>Cookies are domain-scoped by the browser. Call {@code open("/")} (or navigate
 * to the base URL) before {@code restore()} so the driver is on the correct domain
 * when cookies are applied.
 */
@SeleniumBootApi(since = "1.0.0")
public final class SessionCache {

    private static final ConcurrentHashMap<String, SavedSession> CACHE = new ConcurrentHashMap<>();

    private SessionCache() {}

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    /**
     * Captures the current driver's cookies and localStorage and stores them under {@code name}.
     * Overwrites any previously stored session with the same name.
     *
     * @param name logical session name, e.g. {@code "adminSession"}
     */
    public static void store(String name) {
        WebDriver driver = DriverManager.getDriver();
        Set<Cookie> cookies = driver.manage().getCookies();
        Map<String, String> localStorage = captureLocalStorage(driver);
        CACHE.put(name, new SavedSession(cookies, localStorage));
        System.out.println("[SessionCache] Stored session: '" + name + "' (" + cookies.size() + " cookies)");
    }

    /**
     * Restores cookies and localStorage from the named session into the current driver.
     *
     * <p>The driver must already be on the target domain (i.e. you should call
     * {@code open("/")} before this) so the browser accepts the domain-scoped cookies.
     * The page is refreshed after restoration so the app picks up the new session.
     *
     * @param name logical session name previously passed to {@link #store(String)}
     * @return {@code true} if a stored session was found and applied; {@code false} otherwise
     */
    public static boolean restore(String name) {
        SavedSession session = CACHE.get(name);
        if (session == null) {
            System.out.println("[SessionCache] No session found for: '" + name + "'");
            return false;
        }

        WebDriver driver = DriverManager.getDriver();
        driver.manage().deleteAllCookies();
        for (Cookie cookie : session.cookies) {
            try { driver.manage().addCookie(cookie); } catch (Exception ignored) {}
        }
        restoreLocalStorage(driver, session.localStorage);
        driver.navigate().refresh();
        System.out.println("[SessionCache] Restored session: '" + name + "' (" + session.cookies.size() + " cookies)");
        return true;
    }

    /**
     * Returns {@code true} if a session has been stored under {@code name}.
     */
    public static boolean exists(String name) {
        return CACHE.containsKey(name);
    }

    /**
     * Removes the stored session for {@code name}.
     * Subsequent calls to {@link #restore(String)} for this name will return {@code false}.
     */
    public static void invalidate(String name) {
        CACHE.remove(name);
        System.out.println("[SessionCache] Invalidated session: '" + name + "'");
    }

    /**
     * Removes all stored sessions. Typically called in a {@code @AfterSuite} teardown.
     */
    public static void clear() {
        CACHE.clear();
    }

    // ----------------------------------------------------------
    // Internals
    // ----------------------------------------------------------

    private static final class SavedSession {
        final Set<Cookie> cookies;
        final Map<String, String> localStorage;

        SavedSession(Set<Cookie> cookies, Map<String, String> localStorage) {
            this.cookies = cookies;
            this.localStorage = localStorage;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> captureLocalStorage(WebDriver driver) {
        Map<String, String> result = new HashMap<>();
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(
                "var items = {}; " +
                "for (var i = 0; i < localStorage.length; i++) { " +
                "  var k = localStorage.key(i); items[k] = localStorage.getItem(k); " +
                "} return items;"
            );
            if (raw instanceof Map) {
                ((Map<?, ?>) raw).forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static void restoreLocalStorage(WebDriver driver, Map<String, String> items) {
        if (items.isEmpty()) return;
        try {
            StringBuilder js = new StringBuilder("localStorage.clear();");
            items.forEach((k, v) ->
                js.append("localStorage.setItem(")
                  .append(jsString(k)).append(",")
                  .append(jsString(v)).append(");")
            );
            ((JavascriptExecutor) driver).executeScript(js.toString());
        } catch (Exception ignored) {}
    }

    private static String jsString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
