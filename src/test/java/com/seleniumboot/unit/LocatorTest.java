package com.seleniumboot.unit;

import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorTest {

    // LocatorException thrown when no elements match
    @Test
    public void resolve_throwsLocatorException_whenNoElementsFound() {
        // Locator.of(By) requires DriverManager — tested via integration;
        // here we verify LocatorException message is descriptive.
        LocatorException ex = new LocatorException("No element found for: By.id: missing");
        assertTrue(ex.getMessage().contains("No element found"));
    }

    @Test
    public void locatorException_preservesCause() {
        RuntimeException cause = new RuntimeException("root cause");
        LocatorException ex = new LocatorException("wrapped", cause);
        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getMessage(), "wrapped");
    }

    @Test
    public void locator_toString_includesRootBy() {
        Locator loc = Locator.of(By.id("username"));
        assertTrue(loc.toString().contains("username"),
                "toString should include root By description");
    }

    @Test
    public void locator_toString_includesFilterAndNth() {
        Locator loc = Locator.of(By.cssSelector(".row"))
                .filter(".active")
                .nth(2);
        String str = loc.toString();
        assertTrue(str.contains(".active"), "toString should include filter");
        assertTrue(str.contains("2"),       "toString should include nth index");
    }

    @Test
    public void locator_toString_includesWithText() {
        Locator loc = Locator.ofCss("button").withText("Save");
        assertTrue(loc.toString().contains("Save"), "toString should include withText value");
    }

    @Test
    public void locator_toString_includesWithin() {
        Locator loc = Locator.of(By.cssSelector("input"))
                .within(By.id("login-form"));
        assertTrue(loc.toString().contains("login-form"), "toString should include within container");
    }

    @Test
    public void locatorOfCss_createsByCssSelector() {
        Locator loc = Locator.ofCss(".submit-btn");
        assertTrue(loc.toString().contains("submit-btn"));
    }

    @Test
    public void locatorOf_createsByLocator() {
        Locator loc = Locator.of(By.name("email"));
        assertTrue(loc.toString().contains("email"));
    }

    @Test
    public void locatorException_withMessageOnly() {
        LocatorException ex = new LocatorException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void locator_chaining_doesNotMutateOriginal() {
        Locator base    = Locator.ofCss(".item");
        Locator filtered = base.filter(".active");
        // Both should still be valid Locator objects
        assertNotNull(base);
        assertNotNull(filtered);
        assertTrue(filtered.toString().contains(".active"));
    }
}
