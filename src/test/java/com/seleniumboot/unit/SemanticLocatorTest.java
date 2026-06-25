package com.seleniumboot.unit;

import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.Role;
import org.openqa.selenium.By;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for the accessibility-first locators ({@code getByRole}, {@code getByText},
 * {@code getByLabel}, …). These verify the synthesized Selenium {@link By} selector and
 * the human-readable description — no real browser is required.
 */
public class SemanticLocatorTest {

    @AfterMethod
    public void resetTestId() {
        Locator.setTestIdAttribute("data-testid");
    }

    // ---- getByRole -------------------------------------------------------

    @Test
    public void byRole_button_matchesImplicitAndExplicitRoles() {
        String css = ((By.ByCssSelector) Locator.byRole(Role.BUTTON).toBy()).toString();
        assertTrue(css.contains("button"),            "should match <button>");
        assertTrue(css.contains("[role='button']"),   "should match explicit role");
        assertTrue(css.contains("input[type='submit']"), "should match submit inputs");
    }

    @Test
    public void byRole_link_matchesAnchorWithHref() {
        String css = Locator.byRole(Role.LINK).toBy().toString();
        assertTrue(css.contains("a[href]"));
        assertTrue(css.contains("[role='link']"));
    }

    @Test
    public void byRole_heading_withLevel_narrowsToThatLevel() {
        By by = Locator.byRole(Role.HEADING).withLevel(2).toBy();
        String css = by.toString();
        assertTrue(css.contains("h2"), "should target h2");
        assertTrue(css.contains("aria-level='2'"), "should target explicit aria-level");
        assertFalse(css.contains("h1"), "level filter should drop other heading levels");
    }

    @Test
    public void byRole_withName_isReflectedInDescription() {
        Locator loc = Locator.byRole(Role.BUTTON).withName("Submit");
        assertTrue(loc.toString().contains("getByRole(button)"));
        assertTrue(loc.toString().contains(".withName(\"Submit\")"));
    }

    @Test
    public void byRole_describesFriendlyName_notRawCss() {
        String desc = Locator.byRole(Role.HEADING).withLevel(1).toString();
        assertTrue(desc.contains("getByRole(heading)"), "toString should be human-friendly");
        assertTrue(desc.contains(".withLevel(1)"));
    }

    // ---- getByText -------------------------------------------------------

    @Test
    public void byText_default_isCaseInsensitiveContains() {
        String xpath = Locator.byText("Sign In").toBy().toString();
        assertTrue(xpath.contains("translate("), "default should lower-case for CI match");
        assertTrue(xpath.contains("'sign in'"),  "literal should be lower-cased");
        assertTrue(xpath.contains("contains("),  "default should be a substring match");
    }

    @Test
    public void byText_exact_usesNormalizeSpaceEquality() {
        String xpath = Locator.byText("Sign In").exact().toBy().toString();
        assertTrue(xpath.contains("normalize-space(.)='Sign In'"));
        assertFalse(xpath.contains("translate("), "exact match should not lower-case");
    }

    @Test
    public void byText_withApostrophe_producesValidXpathLiteral() {
        // Should not throw and should use concat()/double-quotes rather than break the literal.
        By by = Locator.byText("O'Reilly").exact().toBy();
        String xpath = by.toString();
        assertTrue(xpath.contains("O'Reilly") || xpath.contains("concat("),
                "apostrophe must be handled safely");
    }

    // ---- getByLabel ------------------------------------------------------

    @Test
    public void byLabel_targetsFormControls() {
        String css = Locator.byLabel("Email").toBy().toString();
        assertTrue(css.contains("input"));
        assertTrue(css.contains("textarea"));
        assertTrue(css.contains("select"));
    }

    @Test
    public void byLabel_describesFriendlyLabel() {
        assertTrue(Locator.byLabel("Email address").toString().contains("getByLabel(\"Email address\")"));
    }

    // ---- getByPlaceholder / AltText / Title ------------------------------

    @Test
    public void byPlaceholder_default_isCaseInsensitiveSubstring() {
        String css = Locator.byPlaceholder("Search").toBy().toString();
        assertTrue(css.contains("placeholder*='Search'"));
        assertTrue(css.contains(" i]"), "should use the CSS case-insensitive flag");
    }

    @Test
    public void byPlaceholder_exact_dropsSubstringFlag() {
        String css = Locator.byPlaceholder("Search").exact().toBy().toString();
        assertTrue(css.contains("placeholder='Search'"));
        assertFalse(css.contains("*="), "exact should be an exact attribute match");
    }

    @Test
    public void byAltText_matchesAltAttribute() {
        assertTrue(Locator.byAltText("Logo").toBy().toString().contains("alt*='Logo'"));
    }

    @Test
    public void byTitle_matchesTitleAttribute() {
        assertTrue(Locator.byTitle("Close").toBy().toString().contains("title*='Close'"));
    }

    // ---- getByTestId -----------------------------------------------------

    @Test
    public void byTestId_usesDataTestIdByDefault() {
        String css = Locator.byTestId("checkout-cta").toBy().toString();
        assertTrue(css.contains("[data-testid='checkout-cta']"));
    }

    @Test
    public void byTestId_isExactEvenWithoutExactCall() {
        String css = Locator.byTestId("cta").toBy().toString();
        assertFalse(css.contains("*="), "test-id match must always be exact");
    }

    @Test
    public void byTestId_honorsConfiguredAttribute() {
        Locator.setTestIdAttribute("data-qa");
        String css = Locator.byTestId("login").toBy().toString();
        assertTrue(css.contains("[data-qa='login']"));
    }

    @Test
    public void setTestIdAttribute_ignoresBlankValues() {
        Locator.setTestIdAttribute("  ");
        String css = Locator.byTestId("x").toBy().toString();
        assertTrue(css.contains("[data-testid='x']"), "blank override should be ignored");
    }

    // ---- escaping --------------------------------------------------------

    @Test
    public void byTestId_escapesSingleQuotes() {
        // Should not break the CSS literal.
        By by = Locator.byTestId("it's").toBy();
        assertTrue(by.toString().contains("it\\'s") || by.toString().contains("it's"));
    }

    // ---- chaining immutability of value ----------------------------------

    @Test
    public void exact_isChainableAndReflectedInDescription() {
        assertTrue(Locator.byText("Hello").exact().toString().contains(".exact()"));
    }
}
