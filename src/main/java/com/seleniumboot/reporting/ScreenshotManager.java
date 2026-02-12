package com.seleniumboot.reporting;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles screenshot capture for failed tests.
 */
public final class ScreenshotManager {

    private static final String REPORT_DIR = "target/reports/screenshots";

    private ScreenshotManager() {
    }

    public static void capture(String testName) {
        try {
            WebDriver driver = DriverManager.getDriver();

            if (!(driver instanceof TakesScreenshot)) {
                return;
            }

            File srcFile = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.FILE);

            Path directory = Paths.get(REPORT_DIR);
            Files.createDirectories(directory);

            Path destination = directory.resolve(
                    sanitize(testName) + "_" + System.currentTimeMillis() + ".png"
            );

            Files.copy(srcFile.toPath(), destination);

        } catch (IOException ignored) {
            // Screenshot failures must NOT break test execution
        } catch (Exception ignored) {
            // Defensive safety
        }
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
