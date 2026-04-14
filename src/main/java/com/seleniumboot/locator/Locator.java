package com.seleniumboot.locator;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chainable, auto-waiting element locator.
 *
 * <p>Inspired by Playwright's {@code page.locator()} API. All terminal actions
 * (click, type, getText, …) automatically wait for the element to be in the
 * required state before interacting — no explicit {@code WaitEngine} calls needed.
 *
 * <pre>
 * // In BasePage or BaseTest:
 * $(".row").filter(".active").nth(0).click();
 * $("button").withText("Submit").click();
 * $(By.id("username")).type("admin");
 * assertThat($(".error-msg")).isVisible();
 * </pre>
 */
@SeleniumBootApi(since = "1.4.0")
public final class Locator {

    private final By root;
    private String filterCss;
    private String withText;
    private By withinContainer;
    private int nthIndex = -1;   // -1 = no nth filter

    // ------------------------------------------------------------------
    // Factory — called from BasePage / BaseTest via $()
    // ------------------------------------------------------------------

    public static Locator of(By by) {
        return new Locator(by);
    }

    public static Locator ofCss(String css) {
        return new Locator(By.cssSelector(css));
    }

    private Locator(By root) {
        this.root = root;
    }

    // ------------------------------------------------------------------
    // Chain methods
    // ------------------------------------------------------------------

    /**
     * Narrow results to elements that also match the given CSS selector.
     * <pre>$(".row").filter(".active")</pre>
     */
    public Locator filter(String css) {
        this.filterCss = css;
        return this;
    }

    /**
     * Narrow results to elements whose visible text equals the given string (trimmed).
     * <pre>$("button").withText("Save")</pre>
     */
    public Locator withText(String text) {
        this.withText = text;
        return this;
    }

    /**
     * Scope the search inside a parent container.
     * <pre>$(By.cssSelector("input")).within(By.id("login-form"))</pre>
     */
    public Locator within(By container) {
        this.withinContainer = container;
        return this;
    }

    /**
     * Pick a single element by zero-based index from the matched list.
     * <pre>$(".item").nth(2).getText()</pre>
     */
    public Locator nth(int index) {
        this.nthIndex = index;
        return this;
    }

    // ------------------------------------------------------------------
    // Terminal actions — all auto-wait
    // ------------------------------------------------------------------

    /** Waits for the element to be clickable, then clicks it. */
    public void click() {
        waitForClickable(resolve()).click();
    }

    /** Waits for the element to be visible, clears it, then types the given text. */
    public void type(String text) {
        WebElement el = waitForVisible(resolve());
        el.clear();
        el.sendKeys(text);
    }

    /** Appends text without clearing first. */
    public void append(String text) {
        waitForVisible(resolve()).sendKeys(text);
    }

    /** Waits for the element to be visible and returns its trimmed visible text. */
    public String getText() {
        return waitForVisible(resolve()).getText().trim();
    }

    /** Returns the value of the given attribute, waiting for visibility first. */
    public String getAttribute(String name) {
        return waitForVisible(resolve()).getAttribute(name);
    }

    /** Returns true if the element is present and displayed — does NOT wait. */
    public boolean isVisible() {
        try {
            return resolve().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if the element is absent or not displayed — does NOT wait. */
    public boolean isHidden() {
        return !isVisible();
    }

    /** Returns true if the element is present and enabled. */
    public boolean isEnabled() {
        try {
            return resolve().isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /** Hovers over the element using Actions. */
    public void hover() {
        WebElement el = waitForVisible(resolve());
        new Actions(driver()).moveToElement(el).perform();
    }

    /** Scrolls the element into view using JavaScript. */
    public void scrollIntoView() {
        WebElement el = waitForVisible(resolve());
        ((JavascriptExecutor) driver()).executeScript("arguments[0].scrollIntoView(true);", el);
    }

    /** Clicks using JavaScript — useful when element is obscured. */
    public void jsClick() {
        WebElement el = waitForVisible(resolve());
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", el);
    }

    /** Returns the number of elements currently matching the locator chain (no wait). */
    public int count() {
        return resolveAll().size();
    }

    /** Returns the resolved {@link WebElement}, applying all chain filters. */
    public WebElement element() {
        return waitForVisible(resolve());
    }

    /** Returns all matched {@link WebElement}s, applying all chain filters. */
    public List<WebElement> elements() {
        return resolveAll().stream()
                .filter(WebElement::isDisplayed)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Internal resolution
    // ------------------------------------------------------------------

    private WebElement resolve() {
        List<WebElement> candidates = resolveAll();
        if (candidates.isEmpty()) {
            throw new LocatorException("No element found for: " + describe());
        }
        return candidates.get(0);
    }

    private List<WebElement> resolveAll() {
        WebDriver d = driver();
        List<WebElement> candidates;

        if (withinContainer != null) {
            try {
                WebElement container = d.findElement(withinContainer);
                candidates = container.findElements(root);
            } catch (Exception e) {
                throw new LocatorException("Container not found: " + withinContainer + " — " + e.getMessage());
            }
        } else {
            candidates = d.findElements(root);
        }

        // apply filter
        if (filterCss != null) {
            String css = filterCss;
            candidates = candidates.stream()
                    .filter(el -> {
                        try { return !el.findElements(By.cssSelector("*")).isEmpty()
                                    || matchesCss(el, css); }
                        catch (Exception ignored) { return false; }
                    })
                    .collect(Collectors.toList());

            // simpler: keep elements that themselves match the extra css selector
            // re-query from parent if possible, else filter by attribute
            candidates = filterByCss(d, candidates, css);
        }

        // apply withText
        if (withText != null) {
            String target = withText;
            candidates = candidates.stream()
                    .filter(el -> {
                        try { return el.getText().trim().equals(target); }
                        catch (Exception ignored) { return false; }
                    })
                    .collect(Collectors.toList());
        }

        // apply nth
        if (nthIndex >= 0) {
            if (nthIndex >= candidates.size()) {
                throw new LocatorException(
                        "nth(" + nthIndex + ") requested but only " + candidates.size()
                        + " element(s) matched: " + describe());
            }
            List<WebElement> single = new ArrayList<>();
            single.add(candidates.get(nthIndex));
            return single;
        }

        return candidates;
    }

    /** Filter a candidate list to those whose outerHTML matches a CSS selector via JS. */
    private List<WebElement> filterByCss(WebDriver d, List<WebElement> candidates, String css) {
        JavascriptExecutor js = (JavascriptExecutor) d;
        return candidates.stream().filter(el -> {
            try {
                Object result = js.executeScript(
                        "return arguments[0].matches(arguments[1]);", el, css);
                return Boolean.TRUE.equals(result);
            } catch (Exception ignored) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    private boolean matchesCss(WebElement el, String css) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver();
            Object result = js.executeScript(
                    "return arguments[0].matches(arguments[1]);", el, css);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Wait helpers
    // ------------------------------------------------------------------

    private WebElement waitForVisible(WebElement el) {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(driver(), Duration.ofSeconds(timeout))
                .until(ExpectedConditions.visibilityOf(el));
    }

    private WebElement waitForClickable(WebElement el) {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        return new WebDriverWait(driver(), Duration.ofSeconds(timeout))
                .until(ExpectedConditions.elementToBeClickable(el));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private WebDriver driver() {
        return DriverManager.getDriver();
    }

    private String describe() {
        StringBuilder sb = new StringBuilder(root.toString());
        if (withinContainer != null) sb.append(" within(").append(withinContainer).append(")");
        if (filterCss       != null) sb.append(".filter(\"").append(filterCss).append("\")");
        if (withText        != null) sb.append(".withText(\"").append(withText).append("\")");
        if (nthIndex        >= 0)    sb.append(".nth(").append(nthIndex).append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Locator[" + describe() + "]";
    }
}
