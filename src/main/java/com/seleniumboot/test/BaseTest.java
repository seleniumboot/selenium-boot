package com.seleniumboot.test;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.listeners.SuiteExecutionListener;
import com.seleniumboot.listeners.TestExecutionListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

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
@Listeners({
        SuiteExecutionListener.class,
        TestExecutionListener.class
})
public abstract class BaseTest {

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
