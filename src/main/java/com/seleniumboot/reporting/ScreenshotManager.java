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
import java.util.Base64;

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

    /**
     * Captures a screenshot for the given test and returns the absolute path
     * of the saved file, or {@code null} if capture failed.
     */
    public static String capture(String testName) {
        WebDriver driver = DriverManager.getDriver();

        if (!(driver instanceof TakesScreenshot)) {
            System.err.println("[ScreenshotManager] Driver does not support screenshots for test: " + testName);
            return null;
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
            return destination.toAbsolutePath().toString();

        } catch (WebDriverException | IOException e) {
            System.err.printf("[ScreenshotManager] Failed to capture screenshot for [%s] (%s): %s%n",
                    context, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot and returns it as a base64-encoded PNG string
     * without writing any file to disk. Returns {@code null} if capture failed.
     * Used by {@code StepLogger} for step-level screenshots.
     */
    public static String captureAsBase64() {
        WebDriver driver = DriverManager.getDriver();
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (WebDriverException e) {
            System.err.printf("[ScreenshotManager] captureAsBase64 failed: %s%n", e.getMessage());
            return null;
        }
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
