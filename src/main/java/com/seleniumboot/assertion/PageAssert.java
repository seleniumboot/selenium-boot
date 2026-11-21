package com.seleniumboot.assertion;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Fluent, auto-retrying assertion on the current page's title and URL.
 *
 * <p>Every assertion polls until the condition is true or the configured
 * {@code timeouts.explicit} is exceeded, so it works across redirects and
 * client-side navigation without a manual wait. On failure the message names
 * the last title or URL the browser reported.
 *
 * <p>Obtain an instance via {@link SeleniumAssert#assertThatPage()}, or the
 * {@code assertThatPage()} method on {@code BaseTest}, {@code BasePage} and
 * {@code BaseJUnit5Test}.
 *
 * <pre>
 * assertThatPage().hasTitle("Dashboard");
 * assertThatPage().urlContains("/orders/");
 * assertThatPage().hasUrl(Pattern.compile("https://shop\\.example/orders/\\d+"));
 * </pre>
 */
@SeleniumBootApi(since = "3.13.0")
public final class PageAssert {

    PageAssert() {
    }

    // ------------------------------------------------------------------
    // Title
    // ------------------------------------------------------------------

    /** Asserts the page title equals {@code expected} — retries until timeout. */
    public PageAssert hasTitle(String expected) {
        return check(WebDriver::getTitle, expected::equals,
                "Expected page title [" + expected + "]");
    }

    /** Asserts the page title fully matches {@code pattern} — retries until timeout. */
    public PageAssert hasTitle(Pattern pattern) {
        return check(WebDriver::getTitle, t -> pattern.matcher(t).matches(),
                "Expected page title to match [" + pattern + "]");
    }

    /** Asserts the page title contains {@code fragment} — retries until timeout. */
    public PageAssert titleContains(String fragment) {
        return check(WebDriver::getTitle, t -> t.contains(fragment),
                "Expected page title to contain [" + fragment + "]");
    }

    // ------------------------------------------------------------------
    // URL
    // ------------------------------------------------------------------

    /** Asserts the current URL equals {@code expected} — retries until timeout. */
    public PageAssert hasUrl(String expected) {
        return check(WebDriver::getCurrentUrl, expected::equals,
                "Expected URL [" + expected + "]");
    }

    /** Asserts the current URL fully matches {@code pattern} — retries until timeout. */
    public PageAssert hasUrl(Pattern pattern) {
        return check(WebDriver::getCurrentUrl, u -> pattern.matcher(u).matches(),
                "Expected URL to match [" + pattern + "]");
    }

    /** Asserts the current URL contains {@code fragment} — retries until timeout. */
    public PageAssert urlContains(String fragment) {
        return check(WebDriver::getCurrentUrl, u -> u.contains(fragment),
                "Expected URL to contain [" + fragment + "]");
    }

    // ------------------------------------------------------------------
    // Internal poll
    // ------------------------------------------------------------------

    private PageAssert check(Function<WebDriver, String> read, Predicate<String> condition,
                             String failMessage) {
        int timeout = SeleniumBootContext.getConfig().getTimeouts().getExplicit();
        WebDriver driver = DriverManager.getDriver();
        AtomicReference<String> actual = new AtomicReference<>("not read");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(d -> {
                String value = read.apply(d);
                actual.set(value == null ? "null" : value);
                return value != null && condition.test(value) ? true : null;
            });
        } catch (Exception e) {
            Assert.fail(failMessage + "; actual: [" + actual.get() + "] (timeout: " + timeout + "s)");
        }
        return this;
    }
}
