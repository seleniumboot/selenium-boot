package com.seleniumboot.driver;

import org.openqa.selenium.WebDriver;

/**
 * Strategy interface for WebDriver creation.
 */
public interface DriverProvider {
    WebDriver createDriver();
}
