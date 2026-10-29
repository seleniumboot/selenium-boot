package com.seleniumboot.precondition;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Thread-local cache of browser session state (cookies + localStorage) per condition name.
 *
 * <p>Used by {@link PreConditionRunner} to avoid re-running provider methods when
 * a valid session already exists for the calling thread.
 */
final class SessionCache {

    /** Holds cookies and localStorage snapshot for a single condition. */
    static final class SavedSession {
        final Set<Cookie> cookies;
        final Map<String, String> localStorage;
        final String origin;   // scheme://host[:port] the session was captured on

        SavedSession(Set<Cookie> cookies, Map<String, String> localStorage, String origin) {
            this.cookies = cookies;
            this.localStorage = localStorage;
            this.origin = origin;
        }

        boolean isEmpty() {
            return cookies.isEmpty();
        }
    }

    private static final ThreadLocal<Map<String, SavedSession>> cache =
            ThreadLocal.withInitial(HashMap::new);

    private SessionCache() {}

    /** Captures current cookies + localStorage and stores under the given condition name. */
    static void store(String conditionName, WebDriver driver) {
        Set<Cookie> cookies = driver.manage().getCookies();
        Map<String, String> localStorage = captureLocalStorage(driver);
        cache.get().put(conditionName, new SavedSession(cookies, localStorage, originOf(driver.getCurrentUrl())));
    }

    /**
     * Restores cookies and localStorage from cache into the current browser session.
     * A browser can only accept cookies and localStorage for the origin it is on, and a fresh
     * session starts on {@code about:blank}, so it first navigates to the origin the session
     * was captured on.
     */
    static void restore(String conditionName, WebDriver driver) {
        SavedSession session = cache.get().get(conditionName);
        if (session == null) return;

        if (session.origin != null && !session.origin.equals(originOf(driver.getCurrentUrl()))) {
            driver.get(session.origin);
        }

        driver.manage().deleteAllCookies();
        for (Cookie cookie : session.cookies) {
            try { driver.manage().addCookie(cookie); } catch (Exception ignored) {}
        }

        restoreLocalStorage(driver, session.localStorage);
    }

    /** Returns true if a non-empty cached session exists for the condition and thread. */
    static boolean isValid(String conditionName) {
        SavedSession session = cache.get().get(conditionName);
        return session != null && !session.isEmpty();
    }

    /** Removes the cached session for the given condition on this thread. */
    static void invalidate(String conditionName) {
        cache.get().remove(conditionName);
    }

    /** Clears all cached sessions for the current thread. */
    static void clearAll() {
        cache.get().clear();
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

    /** Returns scheme://host[:port] of an http(s) URL, or null for anything else (about:blank, data:). */
    private static String originOf(String url) {
        try {
            java.net.URI u = java.net.URI.create(url);
            if (u.getScheme() == null || u.getHost() == null
                    || !(u.getScheme().equals("http") || u.getScheme().equals("https"))) return null;
            return u.getScheme() + "://" + u.getHost() + (u.getPort() == -1 ? "" : ":" + u.getPort());
        } catch (Exception e) {
            return null;
        }
    }

    private static String jsString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
