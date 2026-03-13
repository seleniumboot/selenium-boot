package com.seleniumboot.reporting;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles screenshot capture for failed tests.
 *
 * <p>Screenshot failures are logged but never rethrown — a capture error
 * must not mask or alter the actual test failure being reported.
 */
public final class ScreenshotManager {

    private static final String REPORT_DIR = "target/reports/screenshots";

    private ScreenshotManager() {
    }

    public static void capture(String testName) {
        WebDriver driver = DriverManager.getDriver();

        if (!(driver instanceof TakesScreenshot)) {
            System.err.println("[ScreenshotManager] Driver does not support screenshots for test: " + testName);
            return;
        }

        String testId = SeleniumBootContext.getCurrentTestId();
        String context = testId != null ? testId : testName;

        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            Path directory = Paths.get(REPORT_DIR);
            Files.createDirectories(directory);

            Path destination = directory.resolve(
                    sanitize(testName) + "_" + System.currentTimeMillis() + ".png"
            );

            Files.copy(srcFile.toPath(), destination);

        } catch (WebDriverException | IOException e) {
            System.err.printf("[ScreenshotManager] Failed to capture screenshot for [%s] (%s): %s%n",
                    context, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
