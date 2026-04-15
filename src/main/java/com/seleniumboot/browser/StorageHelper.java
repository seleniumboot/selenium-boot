package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Browser storage helpers — localStorage, sessionStorage, and cookies.
 *
 * <p>All localStorage / sessionStorage operations use pure JavaScript and work
 * on every browser. Cookie operations use Selenium's built-in cookie API.
 *
 * <p>Obtain instances via the factory methods {@link #localStorage()},
 * {@link #sessionStorage()}, and {@link #cookies()} — available as protected
 * methods in {@link com.seleniumboot.test.BasePage} and
 * {@link com.seleniumboot.test.BaseTest}.
 *
 * <pre>
 * // localStorage
 * localStorage().set("theme", "dark");
 * String theme = localStorage().get("theme");
 * localStorage().remove("theme");
 * localStorage().clear();
 *
 * // sessionStorage
 * sessionStorage().set("cart", "[{\"id\":1}]");
 *
 * // cookies
 * cookies().set("session_id", "abc123");
 * String sid = cookies().get("session_id");
 * cookies().deleteAll();
 * </pre>
 */
@SeleniumBootApi(since = "1.5.0")
public final class StorageHelper {

    private StorageHelper() {
    }

    // ------------------------------------------------------------------
    // Factory methods
    // ------------------------------------------------------------------

    /** Returns the localStorage helper for the current thread's driver. */
    public static LocalStorage localStorage() {
        return new LocalStorage(DriverManager.getDriver());
    }

    /** Returns the sessionStorage helper for the current thread's driver. */
    public static SessionStorage sessionStorage() {
        return new SessionStorage(DriverManager.getDriver());
    }

    /** Returns the cookie helper for the current thread's driver. */
    public static Cookies cookies() {
        return new Cookies(DriverManager.getDriver());
    }

    // ------------------------------------------------------------------
    // LocalStorage
    // ------------------------------------------------------------------

    /** Provides operations on {@code window.localStorage}. */
    @SeleniumBootApi(since = "1.5.0")
    public static final class LocalStorage {

        private final JavascriptExecutor js;

        private LocalStorage(WebDriver driver) {
            this.js = (JavascriptExecutor) driver;
        }

        /** Sets a key-value pair in localStorage. */
        public void set(String key, String value) {
            js.executeScript("window.localStorage.setItem(arguments[0], arguments[1]);", key, value);
        }

        /** Returns the value for the given key, or {@code null} if not present. */
        public String get(String key) {
            Object result = js.executeScript(
                    "return window.localStorage.getItem(arguments[0]);", key);
            return result != null ? result.toString() : null;
        }

        /** Removes the given key from localStorage. */
        public void remove(String key) {
            js.executeScript("window.localStorage.removeItem(arguments[0]);", key);
        }

        /** Clears all entries from localStorage. */
        public void clear() {
            js.executeScript("window.localStorage.clear();");
        }

        /** Returns the number of entries in localStorage. */
        public int size() {
            Object result = js.executeScript("return window.localStorage.length;");
            return result != null ? ((Long) result).intValue() : 0;
        }

        /** Returns all keys currently in localStorage. */
        @SuppressWarnings("unchecked")
        public List<String> keys() {
            Object result = js.executeScript(
                    "return Object.keys(window.localStorage);");
            if (result instanceof List) return (List<String>) result;
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------
    // SessionStorage
    // ------------------------------------------------------------------

    /** Provides operations on {@code window.sessionStorage}. */
    @SeleniumBootApi(since = "1.5.0")
    public static final class SessionStorage {

        private final JavascriptExecutor js;

        private SessionStorage(WebDriver driver) {
            this.js = (JavascriptExecutor) driver;
        }

        /** Sets a key-value pair in sessionStorage. */
        public void set(String key, String value) {
            js.executeScript("window.sessionStorage.setItem(arguments[0], arguments[1]);", key, value);
        }

        /** Returns the value for the given key, or {@code null} if not present. */
        public String get(String key) {
            Object result = js.executeScript(
                    "return window.sessionStorage.getItem(arguments[0]);", key);
            return result != null ? result.toString() : null;
        }

        /** Removes the given key from sessionStorage. */
        public void remove(String key) {
            js.executeScript("window.sessionStorage.removeItem(arguments[0]);", key);
        }

        /** Clears all entries from sessionStorage. */
        public void clear() {
            js.executeScript("window.sessionStorage.clear();");
        }

        /** Returns the number of entries in sessionStorage. */
        public int size() {
            Object result = js.executeScript("return window.sessionStorage.length;");
            return result != null ? ((Long) result).intValue() : 0;
        }

        /** Returns all keys currently in sessionStorage. */
        @SuppressWarnings("unchecked")
        public List<String> keys() {
            Object result = js.executeScript(
                    "return Object.keys(window.sessionStorage);");
            if (result instanceof List) return (List<String>) result;
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------
    // Cookies
    // ------------------------------------------------------------------

    /** Provides operations on browser cookies via Selenium's cookie API. */
    @SeleniumBootApi(since = "1.5.0")
    public static final class Cookies {

        private final WebDriver driver;

        private Cookies(WebDriver driver) {
            this.driver = driver;
        }

        /** Adds a simple name/value cookie for the current domain. */
        public void set(String name, String value) {
            driver.manage().addCookie(new Cookie(name, value));
        }

        /** Adds a fully configured cookie. */
        public void set(Cookie cookie) {
            driver.manage().addCookie(cookie);
        }

        /** Returns the value of the named cookie, or {@code null} if absent. */
        public String get(String name) {
            Cookie cookie = driver.manage().getCookieNamed(name);
            return cookie != null ? cookie.getValue() : null;
        }

        /** Returns the named {@link Cookie} object, or {@code null} if absent. */
        public Cookie getCookie(String name) {
            return driver.manage().getCookieNamed(name);
        }

        /** Returns all cookies for the current domain. */
        public Set<Cookie> getAll() {
            return driver.manage().getCookies();
        }

        /** Deletes the named cookie. */
        public void delete(String name) {
            driver.manage().deleteCookieNamed(name);
        }

        /** Deletes all cookies for the current domain. */
        public void deleteAll() {
            driver.manage().deleteAllCookies();
        }

        /** Returns {@code true} if a cookie with the given name exists. */
        public boolean exists(String name) {
            return driver.manage().getCookieNamed(name) != null;
        }
    }
}
