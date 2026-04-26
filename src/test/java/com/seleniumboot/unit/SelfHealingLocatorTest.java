package com.seleniumboot.unit;

import com.seleniumboot.healing.SelfHealingLocator;
import com.seleniumboot.healing.SelfHealingLocator.FallbackEntry;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit tests for {@link SelfHealingLocator#buildFallbacks(String)}.
 * Pure logic — no browser required.
 */
public class SelfHealingLocatorTest {

    // ------------------------------------------------------------------
    // CSS selector — id extraction
    // ------------------------------------------------------------------

    @Test
    public void css_hashId_extractsById() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: #login-btn");
        assertContainsStrategy(fb, "id-from-css");
        assertContainsByType(fb, By.id("login-btn"));
    }

    @Test
    public void css_tagPlusHashId_extractsById() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: input#email");
        assertContainsStrategy(fb, "id-from-css");
    }

    @Test
    public void css_nameAttribute_extractsByName() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: [name='username']");
        assertContainsStrategy(fb, "name-from-css");
        assertContainsByType(fb, By.name("username"));
    }

    @Test
    public void css_nameAttributeDoubleQuote_extractsByName() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: [name=\"password\"]");
        assertContainsStrategy(fb, "name-from-css");
        assertContainsByType(fb, By.name("password"));
    }

    @Test
    public void css_className_extractsByLastClass() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: .btn.submit-btn");
        assertContainsStrategy(fb, "class-from-css");
    }

    @Test
    public void css_dataTestId_extractsCssSelector() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: [data-testid='submit']");
        assertContainsStrategy(fb, "data-testid-from-css");
    }

    @Test
    public void css_placeholder_extractsCssSelector() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: [placeholder='Enter email']");
        assertContainsStrategy(fb, "placeholder-from-css");
    }

    // ------------------------------------------------------------------
    // XPath — id / name / text extraction
    // ------------------------------------------------------------------

    @Test
    public void xpath_atId_extractsById() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.xpath: //button[@id='submit']");
        assertContainsStrategy(fb, "id-from-xpath");
        assertContainsByType(fb, By.id("submit"));
    }

    @Test
    public void xpath_atName_extractsByName() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.xpath: //input[@name='email']");
        assertContainsStrategy(fb, "name-from-xpath");
        assertContainsByType(fb, By.name("email"));
    }

    @Test
    public void xpath_exactText_extractsXpathText() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.xpath: //button[text()='Login']");
        assertContainsStrategy(fb, "exact-text-from-xpath");
    }

    @Test
    public void xpath_containsText_extractsXpathContains() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.xpath: //span[contains(text(),'Submit')]");
        assertContainsStrategy(fb, "contains-text-from-xpath");
    }

    // ------------------------------------------------------------------
    // Empty / unknown input
    // ------------------------------------------------------------------

    @Test
    public void empty_locator_returnsEmptyList() {
        Assert.assertTrue(SelfHealingLocator.buildFallbacks("").isEmpty());
    }

    @Test
    public void null_locator_returnsEmptyList() {
        Assert.assertTrue(SelfHealingLocator.buildFallbacks(null).isEmpty());
    }

    @Test
    public void byId_noFallbacksGenerated() {
        // By.id has no CSS/XPath structure to parse — should produce empty list
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.id: login");
        Assert.assertTrue(fb.isEmpty());
    }

    // ------------------------------------------------------------------
    // Multiple fallbacks
    // ------------------------------------------------------------------

    @Test
    public void css_idAndClass_producesTwoFallbacks() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks("By.cssSelector: div#main.container");
        Assert.assertTrue(fb.size() >= 2, "Should produce id-from-css and class-from-css fallbacks");
    }

    @Test
    public void xpath_idAndName_producesTwoFallbacks() {
        List<FallbackEntry> fb = SelfHealingLocator.buildFallbacks(
                "By.xpath: //input[@id='email'][@name='email']");
        // Both id and name should be extracted
        boolean hasId   = fb.stream().anyMatch(e -> "id-from-xpath".equals(e.strategy));
        boolean hasName = fb.stream().anyMatch(e -> "name-from-xpath".equals(e.strategy));
        Assert.assertTrue(hasId && hasName);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void assertContainsStrategy(List<FallbackEntry> fallbacks, String strategy) {
        boolean found = fallbacks.stream().anyMatch(f -> strategy.equals(f.strategy));
        Assert.assertTrue(found, "Expected strategy '" + strategy + "' in fallbacks: "
                + fallbacks.stream().map(f -> f.strategy).toList());
    }

    private void assertContainsByType(List<FallbackEntry> fallbacks, By expected) {
        boolean found = fallbacks.stream().anyMatch(f -> expected.toString().equals(f.by.toString()));
        Assert.assertTrue(found, "Expected By " + expected + " in fallbacks");
    }
}
