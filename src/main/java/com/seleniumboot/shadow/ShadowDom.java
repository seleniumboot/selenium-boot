package com.seleniumboot.shadow;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;

/**
 * Helpers for interacting with Shadow DOM components.
 *
 * <h3>Background</h3>
 * Shadow DOM encapsulates a subtree of the document, making it invisible
 * to standard {@code driver.findElement()} calls. Selenium 4 exposes
 * {@link WebElement#getShadowRoot()} which returns a {@link SearchContext}
 * that supports CSS selectors only — XPath is not supported inside a shadow root.
 *
 * <h3>Quick reference</h3>
 * <pre>
 * // Single shadow root
 * WebElement el = ShadowDom.find(By.css("my-widget"), "#submit");
 *
 * // All matching elements inside a shadow root
 * List&lt;WebElement&gt; items = ShadowDom.findAll(By.css("item-list"), ".item");
 *
 * // Nested shadow roots — pierce through layers
 * WebElement btn = ShadowDom.pierce("outer-shell", "inner-card", "#confirm-btn");
 *
 * // Safe existence check (no exception)
 * boolean present = ShadowDom.exists(host, ".optional-badge");
 * </pre>
 *
 * <p><strong>CSS selectors only</strong> — XPath does not work inside shadow roots.
 */
@SeleniumBootApi(since = "1.2.0")
public final class ShadowDom {

    private ShadowDom() {}

    // ── Single shadow root ─────────────────────────────────────────────────

    /**
     * Finds a single element inside {@code host}'s shadow root using a CSS selector.
     *
     * @param host        the shadow host element (the element that owns the shadow root)
     * @param cssSelector CSS selector scoped to the shadow root
     * @return the matching element
     * @throws NoSuchElementException if no element matches
     */
    public static WebElement find(WebElement host, String cssSelector) {
        return shadowRoot(host).findElement(By.cssSelector(cssSelector));
    }

    /**
     * Locates the shadow host with {@code hostLocator}, then finds a single element
     * inside its shadow root.
     *
     * @param hostLocator locator for the shadow host element
     * @param cssSelector CSS selector scoped to the shadow root
     * @return the matching element
     */
    public static WebElement find(By hostLocator, String cssSelector) {
        return find(driver().findElement(hostLocator), cssSelector);
    }

    /**
     * Finds all elements matching {@code cssSelector} inside {@code host}'s shadow root.
     *
     * @return unmodifiable list; empty if nothing matches
     */
    public static List<WebElement> findAll(WebElement host, String cssSelector) {
        List<WebElement> result = shadowRoot(host).findElements(By.cssSelector(cssSelector));
        return Collections.unmodifiableList(result);
    }

    /**
     * Locates the shadow host with {@code hostLocator}, then finds all elements
     * inside its shadow root.
     *
     * @return unmodifiable list; empty if nothing matches
     */
    public static List<WebElement> findAll(By hostLocator, String cssSelector) {
        return findAll(driver().findElement(hostLocator), cssSelector);
    }

    /**
     * Returns the shadow root {@link SearchContext} of the given host element.
     * Use this when you need raw access to the root (e.g. for custom wait conditions).
     *
     * <p><strong>CSS selectors only</strong> — XPath is not supported inside a shadow root.
     *
     * @param host the shadow host element
     * @return the shadow root search context
     */
    public static SearchContext shadowRoot(WebElement host) {
        return host.getShadowRoot();
    }

    // ── Deep traversal (nested shadow roots) ──────────────────────────────

    /**
     * Traverses a chain of nested shadow roots and returns the final target element.
     *
     * <p>Each selector selects an element within the previous shadow root. The last
     * selector identifies the target; all preceding selectors identify intermediate
     * shadow host elements.
     *
     * <pre>
     * // DOM structure:
     * //  &lt;checkout-flow&gt;             ← shadow host 1
     * //    #shadow-root
     * //      &lt;payment-widget&gt;        ← shadow host 2
     * //        #shadow-root
     * //          #pay-button         ← target
     *
     * WebElement btn = ShadowDom.pierce("checkout-flow", "payment-widget", "#pay-button");
     * </pre>
     *
     * @param cssSelectors at least two selectors: one or more intermediate host selectors
     *                     followed by the final target selector
     * @return the target element at the end of the traversal chain
     * @throws IllegalArgumentException if fewer than 2 selectors are provided
     * @throws NoSuchElementException   if any step of the chain produces no match
     */
    /**
     * Traverses a chain of nested shadow roots and returns the final target element.
     *
     * <p>Uses Selenium 4's native {@link WebElement#getShadowRoot()} chain — more reliable
     * than JavaScript because WebDriver cannot always serialize elements found inside
     * shadow roots back through {@code executeScript} return values.
     *
     * <pre>
     * // DOM structure:
     * //  &lt;checkout-flow&gt;             ← shadow host 1
     * //    #shadow-root
     * //      &lt;payment-widget&gt;        ← shadow host 2
     * //        #shadow-root
     * //          #pay-button         ← target
     *
     * WebElement btn = ShadowDom.pierce("checkout-flow", "payment-widget", "#pay-button");
     * </pre>
     *
     * @param cssSelectors at least two selectors: one or more intermediate host selectors
     *                     followed by the final target selector
     * @return the target element at the end of the traversal chain
     * @throws IllegalArgumentException if fewer than 2 selectors are provided
     * @throws NoSuchElementException   if any step of the chain produces no match
     */
    public static WebElement pierce(String... cssSelectors) {
        if (cssSelectors == null || cssSelectors.length < 2) {
            throw new IllegalArgumentException(
                    "pierce() requires at least 2 CSS selectors: one host selector and one target selector");
        }
        try {
            // Walk from the document-level host element, entering each shadow root in turn
            WebElement current = driver().findElement(By.cssSelector(cssSelectors[0]));
            for (int i = 1; i < cssSelectors.length - 1; i++) {
                current = current.getShadowRoot().findElement(By.cssSelector(cssSelectors[i]));
            }
            return current.getShadowRoot()
                          .findElement(By.cssSelector(cssSelectors[cssSelectors.length - 1]));
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(
                    "Shadow DOM pierce failed — no element found for selector chain: "
                    + String.join(" → ", cssSelectors));
        }
    }

    // ── Existence check ───────────────────────────────────────────────────

    /**
     * Returns {@code true} if at least one element matching {@code cssSelector}
     * exists inside {@code host}'s shadow root.
     * Never throws — returns {@code false} for any error.
     */
    public static boolean exists(WebElement host, String cssSelector) {
        try {
            return !findAll(host, cssSelector).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if at least one element matching {@code cssSelector}
     * exists inside the shadow root of the element found by {@code hostLocator}.
     * Never throws — returns {@code false} for any error.
     */
    public static boolean exists(By hostLocator, String cssSelector) {
        try {
            return !findAll(hostLocator, cssSelector).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private static WebDriver driver() {
        return DriverManager.getDriver();
    }

    /**
     * Builds a self-contained JavaScript snippet that traverses nested shadow roots.
     * Each step after the first queries {@code el.shadowRoot.querySelector(css)}.
     */
    public static String buildPierceJs(String... selectors) {
        StringBuilder js = new StringBuilder(
                "(function() { var el = document.querySelector('" + escapeJs(selectors[0]) + "'); ");
        for (int i = 1; i < selectors.length; i++) {
            js.append("if (!el) return null; ");
            if (i < selectors.length - 1) {
                // Intermediate: move into shadow root then find next host
                js.append("el = el.shadowRoot ? el.shadowRoot.querySelector('")
                  .append(escapeJs(selectors[i])).append("') : null; ");
            } else {
                // Final: find the target inside the last shadow root
                js.append("el = el.shadowRoot ? el.shadowRoot.querySelector('")
                  .append(escapeJs(selectors[i])).append("') : null; ");
            }
        }
        js.append("return el; })()");
        return js.toString();
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
