package com.seleniumboot.pages;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Base class for all Page Objects.
 *
 * <p>Provides driver access and common interaction helpers so page classes
 * don't need to call WaitEngine or DriverManager directly for routine actions.
 * All interactions wait for the element to be ready before acting.
 */
public abstract class BasePage {

    protected final WebDriver driver;

    protected BasePage() {
        this.driver = DriverManager.getDriver();
    }

    // ----------------------------------------------------------
    // Interaction helpers
    // ----------------------------------------------------------

    protected void click(By locator) {
        WaitEngine.waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        WebElement element = WaitEngine.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitEngine.waitForVisible(locator).getText();
    }

    protected String getAttribute(By locator, String attribute) {
        return WaitEngine.waitForVisible(locator).getAttribute(attribute);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }
}
