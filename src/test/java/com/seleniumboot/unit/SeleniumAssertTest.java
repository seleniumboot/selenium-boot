package com.seleniumboot.unit;

import com.seleniumboot.assertion.LocatorAssert;
import com.seleniumboot.assertion.SeleniumAssert;
import com.seleniumboot.locator.Locator;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumAssert} and {@link LocatorAssert}.
 * Verifies factory methods and object contracts — polling behaviour is
 * covered by integration tests that require a real browser.
 */
public class SeleniumAssertTest {

    @Test
    public void assertThat_byLocator_returnsLocatorAssert() {
        LocatorAssert la = SeleniumAssert.assertThat(By.id("username"));
        assertNotNull(la, "assertThat(By) should return a non-null LocatorAssert");
    }

    @Test
    public void assertThat_locator_returnsLocatorAssert() {
        Locator loc = Locator.of(By.cssSelector(".row"));
        LocatorAssert la = SeleniumAssert.assertThat(loc);
        assertNotNull(la, "assertThat(Locator) should return a non-null LocatorAssert");
    }

    @Test
    public void assertThat_differentBy_producesDistinctInstances() {
        LocatorAssert a = SeleniumAssert.assertThat(By.id("a"));
        LocatorAssert b = SeleniumAssert.assertThat(By.id("b"));
        assertNotSame(a, b, "Each call should produce a new LocatorAssert instance");
    }

    @Test
    public void assertThat_cssLocator_returnsLocatorAssert() {
        LocatorAssert la = SeleniumAssert.assertThat(By.cssSelector(".error-msg"));
        assertNotNull(la);
    }

    @Test
    public void assertThat_xpathLocator_returnsLocatorAssert() {
        LocatorAssert la = SeleniumAssert.assertThat(By.xpath("//button[@type='submit']"));
        assertNotNull(la);
    }

    @Test
    public void assertThat_chainedLocator_returnsLocatorAssert() {
        Locator loc = Locator.ofCss("button").withText("Submit").nth(0);
        LocatorAssert la = SeleniumAssert.assertThat(loc);
        assertNotNull(la);
    }

    @Test
    public void locatorAssert_isReturnedFromBaseMethods() {
        // Verify LocatorAssert constructor is accessible from the package
        // (used indirectly via SeleniumAssert factory)
        LocatorAssert la = SeleniumAssert.assertThat(By.name("email"));
        assertNotNull(la);
    }
}
