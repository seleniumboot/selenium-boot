package com.seleniumboot.wait;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
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
                "complete".equals(((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return document.readyState")));
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
