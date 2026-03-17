package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Resilient element resolution that tries multiple locator strategies in order,
 * returning the first element that is found and displayed.
 *
 * <p>Use when an element's locator may differ across environments, browsers,
 * or application versions.
 *
 * <pre>
 * // Try CSS first, fall back to XPath
 * WebElement btn = SmartLocator.find(driver,
 *     By.cssSelector(".submit-btn"),
 *     By.xpath("//button[@type='submit']")
 * );
 *
 * // From inside a BasePage subclass:
 * WebElement el = SmartLocator.find(driver,
 *     By.id("username"),
 *     By.name("user"),
 *     By.cssSelector("input[placeholder='Username']")
 * );
 * </pre>
 */
@SeleniumBootApi(since = "0.8.0")
public final class SmartLocator {

    private SmartLocator() {}

    /**
     * Tries each locator in order and returns the first element that is found and displayed.
     *
     * @param driver   the WebDriver instance
     * @param locators one or more locator strategies to try, in priority order
     * @return the first matching visible element
     * @throws NoSuchElementException if no locator produced a visible element
     */
    public static WebElement find(WebDriver driver, By... locators) {
        if (locators == null || locators.length == 0) {
            throw new IllegalArgumentException("At least one locator must be provided");
        }

        StringBuilder tried = new StringBuilder();
        for (By locator : locators) {
            try {
                WebElement el = driver.findElement(locator);
                if (el.isDisplayed()) {
                    System.out.println("[SmartLocator] Resolved using: " + locator);
                    return el;
                }
            } catch (Exception ignored) {}
            tried.append(locator).append(", ");
        }

        throw new NoSuchElementException(
            "[SmartLocator] No visible element found. Tried: " + tried
        );
    }

    /**
     * Returns {@code true} if any of the given locators produces a visible element.
     * Does not throw.
     *
     * @param driver   the WebDriver instance
     * @param locators one or more locator strategies to try
     */
    public static boolean isAnyVisible(WebDriver driver, By... locators) {
        if (locators == null) return false;
        for (By locator : locators) {
            try {
                if (driver.findElement(locator).isDisplayed()) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
