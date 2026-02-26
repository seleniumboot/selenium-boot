package com.seleniumboot.wait;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralized explicit wait handler.
 *
 * Rules:
 * <li>Explicit waits only</li>
 * <li>No implicit waits</li>
 * <li>Timeout comes from configuration</li>
 */
public final class WaitEngine {

    private WaitEngine() {
    }

    private static WebDriverWait createWait() {
        WebDriver driver = DriverManager.getDriver();
        int timeoutSeconds = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    public static WebElement waitForVisible(By locator) {
        return createWait()
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return createWait()
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForTitle(String title) {
        return createWait()
                .until(ExpectedConditions.titleIs(title));
    }

    public static boolean waitForUrlContains(String partialUrl) {
        return createWait()
                .until(ExpectedConditions.urlContains(partialUrl));
    }
}
