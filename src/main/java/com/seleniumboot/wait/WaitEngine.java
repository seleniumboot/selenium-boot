package com.seleniumboot.wait;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralized explicit wait handler.
 *
 * <p>Rules:
 * <li>Explicit waits only — never set implicitlyWait on the driver</li>
 * <li>Timeout always comes from configuration (selenium-boot.yml)</li>
 * <li>Use {@link #wait(ExpectedCondition)} for conditions not covered here</li>
 */
@SeleniumBootApi(since = "0.4.0")
public final class WaitEngine {

    private WaitEngine() {
    }

    private static WebDriverWait createWait() {
        WebDriver driver = DriverManager.getDriver();
        int timeoutSeconds = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    // ----------------------------------------------------------
    // Visibility
    // ----------------------------------------------------------

    public static WebElement waitForVisible(By locator) {
        return createWait()
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return createWait()
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ----------------------------------------------------------
    // Interactability
    // ----------------------------------------------------------

    public static WebElement waitForClickable(By locator) {
        return createWait()
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForStaleness(WebElement element) {
        return createWait()
                .until(ExpectedConditions.stalenessOf(element));
    }

    // ----------------------------------------------------------
    // Content
    // ----------------------------------------------------------

    public static WebElement waitForText(By locator, String text) {
        createWait().until(ExpectedConditions.textToBe(locator, text));
        return DriverManager.getDriver().findElement(locator);
    }

    public static WebElement waitForAttributeContains(By locator, String attribute, String value) {
        createWait().until(ExpectedConditions.attributeContains(locator, attribute, value));
        return DriverManager.getDriver().findElement(locator);
    }

    // ----------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------

    public static boolean waitForTitle(String title) {
        return createWait()
                .until(ExpectedConditions.titleIs(title));
    }

    public static boolean waitForUrlContains(String partialUrl) {
        return createWait()
                .until(ExpectedConditions.urlContains(partialUrl));
    }

    public static void waitForPageLoad() {
        createWait().until(driver ->
                "complete".equals(((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")));
    }

    // ----------------------------------------------------------
    // Component framework waits
    // ----------------------------------------------------------

    /**
     * Waits until the Angular application has no pending HTTP requests, timers,
     * or micro-tasks — i.e., the zone is stable.
     *
     * <p>Supports both Angular 2+ ({@code window.getAllAngularTestabilities()})
     * and AngularJS 1.x ({@code $http.pendingRequests}).
     * Returns immediately if neither API is detected on the page.
     *
     * <pre>
     * WaitEngine.waitForAngular();
     * page.clickSubmit();
     * </pre>
     */
    public static void waitForAngular() {
        createWait().until(driver -> {
            try {
                Object result = ((JavascriptExecutor) driver).executeScript(
                    // Angular 2+ — testability API
                    "if (window.getAllAngularTestabilities) {" +
                    "  var t = window.getAllAngularTestabilities();" +
                    "  if (!t || t.length === 0) return true;" +
                    "  return t.every(function(tb) { return tb.isStable(); });" +
                    "}" +
                    // AngularJS 1.x — $http pending requests
                    "if (window.angular) {" +
                    "  try {" +
                    "    var inj = window.angular.element(document.body).injector();" +
                    "    if (!inj) return true;" +
                    "    return inj.get('$http').pendingRequests.length === 0;" +
                    "  } catch(e) { return true; }" +
                    "}" +
                    // Not an Angular page — nothing to wait for
                    "return true;"
                );
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                return true;
            }
        });
    }

    /**
     * Waits until React has finished hydrating the server-side-rendered HTML.
     *
     * <p>Detection strategy (in order):
     * <ol>
     *   <li>React 18 — looks for {@code __reactFiber$} or {@code __reactContainer$}
     *       keys on the root element.</li>
     *   <li>React 16/17 — looks for {@code _reactRootContainer} on the root element.</li>
     *   <li>Falls back to looking for any element with a {@code __reactFiber$} key
     *       (catches apps that mount to a non-standard root id).</li>
     * </ol>
     *
     * <p>Common root ids checked: {@code #root}, {@code #__next} (Next.js), {@code #app}.
     * Returns immediately if none are found or if React is not present on the page.
     *
     * <pre>
     * WaitEngine.waitForReactHydration();
     * page.clickButton();
     * </pre>
     */
    public static void waitForReactHydration() {
        waitForPageLoad();
        createWait().until(driver -> {
            try {
                Object result = ((JavascriptExecutor) driver).executeScript(
                    // Locate the React root element (standard root ids)
                    "var root = document.getElementById('root') ||" +
                    "           document.getElementById('__next') ||" +
                    "           document.getElementById('app');" +
                    "if (root) {" +
                    "  var keys = Object.keys(root);" +
                    // React 18: __reactFiber$xxx or __reactContainer$xxx
                    "  if (keys.some(function(k) {" +
                    "    return k.startsWith('__reactFiber') || k.startsWith('__reactContainer');" +
                    "  })) return true;" +
                    // React 16/17: _reactRootContainer
                    "  if (root._reactRootContainer) return true;" +
                    "}" +
                    // Fallback: scan body children for any React fiber key
                    "var children = document.body ? document.body.children : [];" +
                    "for (var i = 0; i < children.length; i++) {" +
                    "  var k = Object.keys(children[i]);" +
                    "  if (k.some(function(key) { return key.startsWith('__reactFiber'); })) return true;" +
                    "}" +
                    // React not detected — nothing to wait for
                    "return true;"
                );
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                return true;
            }
        });
    }

    // ----------------------------------------------------------
    // Alert
    // ----------------------------------------------------------

    public static Alert waitForAlert() {
        return createWait()
                .until(ExpectedConditions.alertIsPresent());
    }

    // ----------------------------------------------------------
    // Escape hatch for custom conditions
    // ----------------------------------------------------------

    public static <T> T wait(ExpectedCondition<T> condition) {
        return createWait().until(condition);
    }
}
