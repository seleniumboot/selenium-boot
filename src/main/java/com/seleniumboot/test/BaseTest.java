package com.seleniumboot.test;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.WebDriver;

/**
 * BaseTest is the mandatory superclass for all Selenium Boot tests.
 *
 * Responsibilities:
 * - Provide access to the framework-managed WebDriver
 *
 * Rules:
 * - Tests must NOT create or quit WebDriver
 * - Tests must NOT manage waits or retries
 */
public abstract class BaseTest {

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
