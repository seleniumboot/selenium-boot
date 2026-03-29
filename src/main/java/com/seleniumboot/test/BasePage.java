package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.assertion.SoftAssertionCollector;
import com.seleniumboot.assertion.SoftAssertions;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.shadow.ShadowDom;
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

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

    /** Tracks how many frames deep we are on this thread — used to decide parentFrame vs defaultContent. */
    private static final ThreadLocal<Integer> FRAME_DEPTH = ThreadLocal.withInitial(() -> 0);

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
    // Dropdown helpers (HTML <select>)
    // ----------------------------------------------------------

    /**
     * Selects an option from a {@code <select>} element by its visible text.
     *
     * <pre>selectByText(By.id("country"), "United Kingdom");</pre>
     */
    protected void selectByText(By locator, String text) {
        new Select(WaitEngine.waitForVisible(locator)).selectByVisibleText(text);
    }

    /**
     * Selects an option from a {@code <select>} element by its {@code value} attribute.
     *
     * <pre>selectByValue(By.id("status"), "active");</pre>
     */
    protected void selectByValue(By locator, String value) {
        new Select(WaitEngine.waitForVisible(locator)).selectByValue(value);
    }

    /**
     * Selects an option from a {@code <select>} element by its zero-based index.
     *
     * <pre>selectByIndex(By.id("month"), 2);</pre>
     */
    protected void selectByIndex(By locator, int index) {
        new Select(WaitEngine.waitForVisible(locator)).selectByIndex(index);
    }

    /**
     * Returns the visible text of the currently selected option in a {@code <select>} element.
     */
    protected String getSelectedOption(By locator) {
        return new Select(WaitEngine.waitForVisible(locator)).getFirstSelectedOption().getText();
    }

    // ----------------------------------------------------------
    // Alert helpers
    // ----------------------------------------------------------

    /**
     * Waits for a browser alert to be present, then accepts it (clicks OK).
     */
    protected void acceptAlert() {
        waitForAlert().accept();
    }

    /**
     * Waits for a browser alert to be present, then dismisses it (clicks Cancel).
     */
    protected void dismissAlert() {
        waitForAlert().dismiss();
    }

    /**
     * Waits for a browser alert to be present and returns its text.
     */
    protected String getAlertText() {
        return waitForAlert().getText();
    }

    /**
     * Waits for a browser alert to be present, captures its text, accepts it,
     * and returns the text in one step.
     *
     * <pre>String msg = getAndAcceptAlert();</pre>
     */
    protected String getAndAcceptAlert() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    /**
     * Waits for a prompt alert, types the given text into it, then accepts it.
     *
     * <pre>typeInAlert("my input");</pre>
     */
    protected void typeInAlert(String text) {
        Alert alert = waitForAlert();
        alert.sendKeys(text);
        alert.accept();
    }

    private Alert waitForAlert() {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.alertIsPresent());
    }

    // ----------------------------------------------------------
    // Mouse action helpers
    // ----------------------------------------------------------

    /**
     * Moves the mouse over the element (hover / mouse-over).
     *
     * <pre>hover(By.id("menu-item"));</pre>
     */
    protected void hover(By locator) {
        WebElement el = WaitEngine.waitForVisible(locator);
        new Actions(driver).moveToElement(el).perform();
    }

    /**
     * Double-clicks the element.
     */
    protected void doubleClick(By locator) {
        WebElement el = WaitEngine.waitForClickable(locator);
        new Actions(driver).doubleClick(el).perform();
    }

    /**
     * Right-clicks (context menu) the element.
     */
    protected void rightClick(By locator) {
        WebElement el = WaitEngine.waitForVisible(locator);
        new Actions(driver).contextClick(el).perform();
    }

    // ----------------------------------------------------------
    // Scroll helpers
    // ----------------------------------------------------------

    /**
     * Scrolls the element into the visible viewport.
     *
     * <pre>scrollTo(By.id("footer"));</pre>
     */
    protected void scrollTo(By locator) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    /**
     * Scrolls the page to the very top.
     */
    protected void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Scrolls the page to the very bottom.
     */
    protected void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    // ----------------------------------------------------------
    // JavaScript fallback helpers
    // ----------------------------------------------------------

    /**
     * Clicks the element via JavaScript — useful when a native click is blocked by an overlay.
     *
     * <pre>jsClick(By.id("hidden-trigger"));</pre>
     */
    protected void jsClick(By locator) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    /**
     * Sets the element's {@code value} property via JavaScript — useful for read-only inputs
     * or custom components that block native {@code sendKeys}.
     *
     * <pre>jsType(By.id("date-picker"), "2025-01-01");</pre>
     */
    protected void jsType(By locator, String text) {
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", el, text);
    }

    // ----------------------------------------------------------
    // Soft assertions
    // ----------------------------------------------------------

    /**
     * Returns the soft assertion collector for this test.
     * Failures collected here are reported all-at-once at test end
     * without interrupting test execution.
     *
     * <pre>
     * softAssert().that(getText(TITLE).equals("Dashboard"), "Title mismatch");
     * softAssert().that(isDisplayed(MENU), "Menu should be visible");
     * </pre>
     */
    protected SoftAssertionCollector softAssert() {
        return SoftAssertions.get();
    }

    // ----------------------------------------------------------
    // SmartLocator helper
    // ----------------------------------------------------------

    /**
     * Tries each locator in order and returns the first element that is found and displayed.
     *
     * <p>Delegates to {@link SmartLocator#find(WebDriver, By...)} — no need to pass the driver manually.
     *
     * <pre>
     * WebElement btn = smartFind(
     *     By.cssSelector(".submit-btn"),
     *     By.xpath("//button[@type='submit']")
     * );
     * </pre>
     *
     * @param primary   the preferred locator strategy
     * @param fallbacks additional strategies tried in order if the primary fails
     * @return the first matching visible element
     */
    protected WebElement smartFind(By primary, By... fallbacks) {
        By[] all = new By[1 + fallbacks.length];
        all[0] = primary;
        System.arraycopy(fallbacks, 0, all, 1, fallbacks.length);
        return SmartLocator.find(driver, all);
    }

    // ----------------------------------------------------------
    // iFrame helpers
    // ----------------------------------------------------------

    /**
     * Switches into the given frame, runs the action, then restores the previous context.
     * Safe to nest — inner frames restore to their parent frame, not default content.
     *
     * <pre>
     * withinFrame(By.id("outer-iframe"), () -> {
     *     withinFrame(By.id("inner-iframe"), () -> {
     *         type(By.id("card-number"), "4111111111111111");
     *     });
     *     click(By.id("pay")); // still inside outer-iframe
     * });
     * </pre>
     */
    protected void withinFrame(By frameLocator, Runnable action) {
        WebElement frame = WaitEngine.waitForVisible(frameLocator);
        driver.switchTo().frame(frame);
        FRAME_DEPTH.set(FRAME_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            exitFrame();
        }
    }

    /**
     * Switches into the frame at the given zero-based index, runs the action,
     * then restores the previous context. Safe to nest.
     */
    protected void withinFrameIndex(int index, Runnable action) {
        driver.switchTo().frame(index);
        FRAME_DEPTH.set(FRAME_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            exitFrame();
        }
    }

    /**
     * Switches into the frame identified by name or id attribute, runs the action,
     * then restores the previous context. Safe to nest.
     */
    protected void withinFrameName(String nameOrId, Runnable action) {
        driver.switchTo().frame(nameOrId);
        FRAME_DEPTH.set(FRAME_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            exitFrame();
        }
    }

    private void exitFrame() {
        int depth = FRAME_DEPTH.get() - 1;
        FRAME_DEPTH.set(depth);
        if (depth == 0) {
            driver.switchTo().defaultContent();
        } else {
            driver.switchTo().parentFrame();
        }
    }

    // ----------------------------------------------------------
    // Shadow DOM helpers
    // ----------------------------------------------------------

    /**
     * Finds a single element inside the shadow root of the element at {@code hostLocator}.
     *
     * <pre>
     * WebElement input = shadowFind(By.cssSelector("my-form"), "#email");
     * </pre>
     *
     * @param hostLocator locator for the shadow host element
     * @param innerCss    CSS selector scoped to the shadow root (XPath not supported)
     */
    protected WebElement shadowFind(By hostLocator, String innerCss) {
        return ShadowDom.find(hostLocator, innerCss);
    }

    /**
     * Finds all elements matching {@code innerCss} inside the shadow root of {@code hostLocator}.
     *
     * @return unmodifiable list; empty if nothing matches
     */
    protected java.util.List<WebElement> shadowFindAll(By hostLocator, String innerCss) {
        return ShadowDom.findAll(hostLocator, innerCss);
    }

    /**
     * Clicks an element inside a shadow root.
     *
     * <pre>shadowClick(By.cssSelector("my-form"), "#submit-btn");</pre>
     */
    protected void shadowClick(By hostLocator, String innerCss) {
        ShadowDom.find(hostLocator, innerCss).click();
    }

    /**
     * Clears and types text into an input inside a shadow root.
     *
     * <pre>shadowType(By.cssSelector("my-form"), "#email", "user@example.com");</pre>
     */
    protected void shadowType(By hostLocator, String innerCss, String text) {
        WebElement el = ShadowDom.find(hostLocator, innerCss);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Returns the visible text of an element inside a shadow root.
     */
    protected String shadowGetText(By hostLocator, String innerCss) {
        return ShadowDom.find(hostLocator, innerCss).getText();
    }

    /**
     * Traverses nested shadow roots and returns the target element.
     * Pass CSS selectors from outermost host down to the target element.
     *
     * <pre>
     * // &lt;checkout-flow&gt; → shadow → &lt;payment-widget&gt; → shadow → #pay-btn
     * WebElement btn = shadowPierce("checkout-flow", "payment-widget", "#pay-btn");
     * </pre>
     */
    protected WebElement shadowPierce(String... cssSelectors) {
        return ShadowDom.pierce(cssSelectors);
    }

    /**
     * Returns {@code true} if at least one element matching {@code innerCss}
     * exists inside the host's shadow root. Never throws.
     */
    protected boolean shadowExists(By hostLocator, String innerCss) {
        return ShadowDom.exists(hostLocator, innerCss);
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
