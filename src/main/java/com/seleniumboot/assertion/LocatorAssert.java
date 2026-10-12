package com.seleniumboot.assertion;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Fluent, auto-retrying assertion for a specific locator.
 *
 * <p>Every assertion polls via {@link WebDriverWait} until the condition is true
 * or the configured {@code timeouts.explicit} is exceeded — just like Playwright's
 * {@code expect(locator).toBeVisible()}.
 *
 * <p>Obtain an instance via {@link SeleniumAssert#assertThat(By)} or
 * {@link SeleniumAssert#assertThat(Locator)}.
 *
 * <pre>
 * assertThat(By.id("status")).hasText("Active");
 * assertThat(By.cssSelector(".error")).isVisible();
 * assertThat($("button").withText("Submit")).isEnabled();
 * </pre>
 */
@SeleniumBootApi(since = "1.4.0")
public final class LocatorAssert {

    private final By by;
    private final String description;

    LocatorAssert(By by, String description) {
        this.by          = by;
        this.description = description;
    }

    // ------------------------------------------------------------------
    // Visibility
    // ------------------------------------------------------------------

    /** Asserts element is present and visible — retries until timeout. */
    public LocatorAssert isVisible() {
        poll(ExpectedConditions.visibilityOfElementLocated(by),
                "Expected element to be visible: " + description);
        return this;
    }

    /** Asserts element is absent or not visible — retries until timeout. */
    public LocatorAssert isHidden() {
        poll(ExpectedConditions.invisibilityOfElementLocated(by),
                "Expected element to be hidden: " + description);
        return this;
    }

    // ------------------------------------------------------------------
    // Interactability
    // ------------------------------------------------------------------

    /** Asserts element is present, visible, and enabled — retries until timeout. */
    public LocatorAssert isEnabled() {
        poll(driver -> {
            List<WebElement> els = driver.findElements(by);
            if (els.isEmpty()) return null;
            WebElement el = els.get(0);
            return el.isDisplayed() && el.isEnabled() ? true : null;
        }, "Expected element to be enabled: " + description);
        return this;
    }

    /** Asserts element is present but disabled — retries until timeout. */
    public LocatorAssert isDisabled() {
        poll(driver -> {
            List<WebElement> els = driver.findElements(by);
            if (els.isEmpty()) return null;
            WebElement el = els.get(0);
            return el.isDisplayed() && !el.isEnabled() ? true : null;
        }, "Expected element to be disabled: " + description);
        return this;
    }

    /** Asserts a checkbox or radio button is checked — retries until timeout. */
    public LocatorAssert isChecked() {
        poll(driver -> {
            List<WebElement> els = driver.findElements(by);
            if (els.isEmpty()) return null;
            return els.get(0).isSelected() ? true : null;
        }, "Expected element to be checked: " + description);
        return this;
    }

    /** Asserts the element owns browser focus — retries until timeout. */
    public LocatorAssert isFocused() {
        AtomicReference<String> actual = new AtomicReference<>("no matching element");
        poll(driver -> {
            try {
                List<WebElement> els = driver.findElements(by);
                if (els.isEmpty()) return null;
                WebElement focused = driver.switchTo().activeElement();
                actual.set(describeElement(focused));
                return els.get(0).equals(focused) ? true : null;
            } catch (StaleElementReferenceException e) {
                actual.set("stale element");
                return null;
            }
        }, "Expected element to be focused: " + description, actual::get);
        return this;
    }

    /**
     * Asserts the element is empty — inputs use their {@code value}; other
     * elements use visible text. Retries until timeout.
     */
    public LocatorAssert isEmpty() {
        AtomicReference<String> actual = new AtomicReference<>("no matching element");
        poll(driver -> {
            try {
                List<WebElement> els = driver.findElements(by);
                if (els.isEmpty()) return null;
                WebElement el = els.get(0);
                String tag = el.getTagName();
                String content = isFormValueElement(tag) ? el.getAttribute("value") : el.getText();
                actual.set(content == null ? "null" : content);
                return content == null || content.isEmpty() ? true : null;
            } catch (StaleElementReferenceException e) {
                actual.set("stale element");
                return null;
            }
        }, "Expected element to be empty: " + description, actual::get);
        return this;
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    /** Asserts the element's visible text equals {@code expected} (trimmed) — retries until timeout. */
    public LocatorAssert hasText(String expected) {
        poll(ExpectedConditions.textToBe(by, expected),
                "Expected text [" + expected + "] for: " + description);
        return this;
    }

    /** Asserts the element's visible text fully matches {@code pattern} — retries until timeout. */
    public LocatorAssert hasText(Pattern pattern) {
        return matches(pattern, false, false);
    }

    /** Asserts the element's visible text contains {@code fragment} — retries until timeout. */
    public LocatorAssert containsText(String fragment) {
        poll(ExpectedConditions.textToBePresentInElementLocated(by, fragment),
                "Expected text to contain [" + fragment + "] for: " + description);
        return this;
    }

    /** Asserts some part of the element's visible text matches {@code pattern} — retries until timeout. */
    public LocatorAssert containsText(Pattern pattern) {
        return matches(pattern, true, false);
    }

    // ------------------------------------------------------------------
    // Attributes & CSS
    // ------------------------------------------------------------------

    /** Asserts the element's {@code value} attribute equals {@code expected} — retries until timeout. */
    public LocatorAssert hasValue(String expected) {
        poll(ExpectedConditions.attributeToBe(by, "value", expected),
                "Expected value [" + expected + "] for: " + description);
        return this;
    }

    /** Asserts the element's {@code value} fully matches {@code pattern} — retries until timeout. */
    public LocatorAssert hasValue(Pattern pattern) {
        return matches(pattern, false, true);
    }

    /** Asserts the element has a specific attribute value — retries until timeout. */
    public LocatorAssert hasAttribute(String attribute, String expected) {
        poll(ExpectedConditions.attributeToBe(by, attribute, expected),
                "Expected attribute [" + attribute + "=" + expected + "] for: " + description);
        return this;
    }

    /** Asserts the element has the given CSS class — retries until timeout. */
    public LocatorAssert hasClass(String className) {
        poll(driver -> {
            List<WebElement> els = driver.findElements(by);
            if (els.isEmpty()) return null;
            String classes = els.get(0).getAttribute("class");
            if (classes == null) return null;
            for (String cls : classes.split("\\s+")) {
                if (cls.equals(className)) return true;
            }
            return null;
        }, "Expected element to have class [" + className + "]: " + description);
        return this;
    }

    // ------------------------------------------------------------------
    // Count
    // ------------------------------------------------------------------

    /** Asserts the number of matching elements equals {@code expected} — retries until timeout. */
    public LocatorAssert count(int expected) {
        poll(ExpectedConditions.numberOfElementsToBe(by, expected),
                "Expected " + expected + " element(s) for: " + description);
        return this;
    }

    // ------------------------------------------------------------------
    // Internal poll
    // ------------------------------------------------------------------

    private <T> void poll(ExpectedCondition<T> condition, String failMessage) {
        poll(condition, failMessage, null);
    }

    private <T> void poll(ExpectedCondition<T> condition, String failMessage,
                          Supplier<String> actualValue) {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        WebDriver driver = DriverManager.getDriver();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(condition);
        } catch (Exception e) {
            String actual = actualValue == null ? "" : "; actual: [" + actualValue.get() + "]";
            Assert.fail(failMessage + actual + " (timeout: " + timeout + "s)");
        }
    }

    private LocatorAssert matches(Pattern pattern, boolean find, boolean valueAttribute) {
        AtomicReference<String> actual = new AtomicReference<>("no matching element");
        String subject = valueAttribute ? "value" : "text";
        poll(driver -> {
            try {
                List<WebElement> els = driver.findElements(by);
                if (els.isEmpty()) return null;
                String candidate = valueAttribute
                        ? els.get(0).getAttribute("value")
                        : els.get(0).getText();
                actual.set(candidate == null ? "null" : candidate);
                if (candidate == null) return null;
                var matcher = pattern.matcher(candidate);
                return (find ? matcher.find() : matcher.matches()) ? true : null;
            } catch (StaleElementReferenceException e) {
                actual.set("stale element");
                return null;
            }
        }, "Expected " + subject + " to " + (find ? "contain a match for " : "match ")
                + "[" + pattern + "] for: " + description, actual::get);
        return this;
    }

    private static boolean isFormValueElement(String tag) {
        return "input".equalsIgnoreCase(tag)
                || "textarea".equalsIgnoreCase(tag)
                || "select".equalsIgnoreCase(tag);
    }

    private static String describeElement(WebElement element) {
        if (element == null) return "none";
        try {
            String tag = element.getTagName();
            String id = element.getAttribute("id");
            return id == null || id.isEmpty() ? "<" + tag + ">" : "<" + tag + " id=\"" + id + "\">";
        } catch (StaleElementReferenceException e) {
            return "stale element";
        }
    }
}
