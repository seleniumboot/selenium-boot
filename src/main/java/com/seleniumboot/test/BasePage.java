package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.nio.file.Paths;

/**
 * Base class for all page objects.
 *
 * <p>Provides safe, wait-backed helpers so page objects never call raw
 * Selenium APIs directly. Extend this class instead of writing boilerplate
 * in every page object.
 *
 * <pre>
 * public class LoginPage extends BasePage {
 *     private static final By USERNAME = By.id("username");
 *     private static final By PASSWORD = By.id("password");
 *     private static final By SUBMIT   = By.id("submit");
 *
 *     public LoginPage(WebDriver driver) { super(driver); }
 *
 *     public void login(String user, String pass) {
 *         type(USERNAME, user);
 *         type(PASSWORD, pass);
 *         click(SUBMIT);
 *     }
 * }
 * </pre>
 */
@SeleniumBootApi(since = "0.8.0")
public abstract class BasePage {

    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    // ----------------------------------------------------------
    // Core interaction helpers
    // ----------------------------------------------------------

    /**
     * Waits for the element to be clickable, then clicks it.
     */
    protected void click(By locator) {
        WaitEngine.waitForClickable(locator).click();
    }

    /**
     * Waits for the element to be visible, clears it, then types the given text.
     */
    protected void type(By locator, String text) {
        WebElement el = WaitEngine.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Waits for the element to be visible and returns its visible text.
     */
    protected String getText(By locator) {
        return WaitEngine.waitForVisible(locator).getText();
    }

    /**
     * Waits for the element to be visible and returns the value of the given attribute.
     */
    protected String getAttribute(By locator, String attribute) {
        return WaitEngine.waitForVisible(locator).getAttribute(attribute);
    }

    /**
     * Returns {@code true} if the element is present in the DOM and visible.
     * Does not throw — returns {@code false} for missing or hidden elements.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ----------------------------------------------------------
    // iFrame helpers
    // ----------------------------------------------------------

    /**
     * Switches into the given frame, runs the action, then switches back to default content.
     * Restores default content even if the action throws.
     *
     * <pre>
     * withinFrame(By.id("payment-iframe"), () -> {
     *     type(By.id("card-number"), "4111111111111111");
     *     click(By.id("pay"));
     * });
     * </pre>
     */
    protected void withinFrame(By frameLocator, Runnable action) {
        WebElement frame = WaitEngine.waitForVisible(frameLocator);
        driver.switchTo().frame(frame);
        try {
            action.run();
        } finally {
            driver.switchTo().defaultContent();
        }
    }

    /**
     * Switches into the frame at the given zero-based index, runs the action,
     * then switches back to default content.
     * Restores default content even if the action throws.
     */
    protected void withinFrameIndex(int index, Runnable action) {
        driver.switchTo().frame(index);
        try {
            action.run();
        } finally {
            driver.switchTo().defaultContent();
        }
    }

    // ----------------------------------------------------------
    // File upload
    // ----------------------------------------------------------

    /**
     * Sends the given file path to a file input element.
     *
     * <p>The {@code filePath} is resolved in this order:
     * <ol>
     *   <li>Absolute path — used as-is if the file exists.</li>
     *   <li>Classpath resource — resolved relative to {@code src/test/resources/}.</li>
     *   <li>Project-root relative path — resolved from the current working directory.</li>
     * </ol>
     *
     * <pre>
     * upload(By.id("file-input"), "testfiles/sample.pdf");
     * upload(By.id("avatar"),     "/absolute/path/to/image.png");
     * </pre>
     */
    protected void upload(By inputLocator, String filePath) {
        String absolutePath = resolveFilePath(filePath);
        WebElement input = WaitEngine.waitForVisible(inputLocator);
        input.sendKeys(absolutePath);
    }

    private String resolveFilePath(String filePath) {
        // 1. Absolute path
        File absolute = new File(filePath);
        if (absolute.isAbsolute() && absolute.exists()) {
            return absolute.getAbsolutePath();
        }

        // 2. Classpath resource (src/test/resources)
        java.net.URL resource = getClass().getClassLoader().getResource(filePath);
        if (resource != null) {
            try {
                return Paths.get(resource.toURI()).toAbsolutePath().toString();
            } catch (Exception ignored) {}
        }

        // 3. Project-root relative
        File relative = Paths.get(System.getProperty("user.dir"), filePath).toFile();
        if (relative.exists()) {
            return relative.getAbsolutePath();
        }

        throw new IllegalArgumentException(
            "File not found for upload: '" + filePath + "'. " +
            "Checked: absolute path, classpath resources, and project-root relative path."
        );
    }
}
