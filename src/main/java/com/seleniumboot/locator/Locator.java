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

    /** How the base set of candidate elements is derived. */
    private enum Kind { CSS_OR_BY, ROLE, TEXT, LABEL, PLACEHOLDER, TESTID, ALT_TEXT, TITLE }

    /** Default attribute used by {@link #byTestId(String)} — overridable. */
    private static volatile String testIdAttribute = "data-testid";

    /** Form controls that can carry an accessible name from an associated label. */
    private static final String FORM_CONTROL_CSS =
            "input:not([type='hidden']):not([type='button']):not([type='submit']):not([type='reset']), "
            + "textarea, select, [contenteditable='true'], "
            + "[role='textbox'], [role='searchbox'], [role='combobox'], [role='checkbox'], "
            + "[role='radio'], [role='switch'], [role='slider'], [role='spinbutton']";

    private final Kind kind;
    private final By root;            // set only for Kind.CSS_OR_BY
    private Role semanticRole;        // set only for Kind.ROLE
    private String semanticValue;     // raw text/attr value for the semantic kinds
    private Integer headingLevel;     // optional heading level filter
    private String accessibleName;    // withName(...) filter for role/label kinds
    private boolean exact;            // exact (case-sensitive) vs substring (case-insensitive)

    private String filterCss;
    private String withText;
    private By withinContainer;
    private int nthIndex = -1;   // -1 = no nth filter

    // ------------------------------------------------------------------
    // Factory — called from BasePage / BaseTest via $() and getBy*()
    // ------------------------------------------------------------------

    public static Locator of(By by) {
        return new Locator(by);
    }

    public static Locator ofCss(String css) {
        return new Locator(By.cssSelector(css));
    }

    /** Locates elements by their ARIA role (implicit or explicit). */
    public static Locator byRole(Role role) {
        Locator l = new Locator(Kind.ROLE);
        l.semanticRole = role;
        return l;
    }

    /** Locates elements by visible text — case-insensitive substring by default. */
    public static Locator byText(String text) {
        return semantic(Kind.TEXT, text);
    }

    /** Locates a form control by its associated label text. */
    public static Locator byLabel(String label) {
        Locator l = semantic(Kind.LABEL, null);
        l.accessibleName = label;
        return l;
    }

    /** Locates an element by its {@code placeholder} attribute. */
    public static Locator byPlaceholder(String placeholder) {
        return semantic(Kind.PLACEHOLDER, placeholder);
    }

    /** Locates an element by its test-id attribute (default {@code data-testid}). */
    public static Locator byTestId(String testId) {
        return semantic(Kind.TESTID, testId);
    }

    /** Locates an element (typically {@code <img>}) by its {@code alt} text. */
    public static Locator byAltText(String altText) {
        return semantic(Kind.ALT_TEXT, altText);
    }

    /** Locates an element by its {@code title} attribute. */
    public static Locator byTitle(String title) {
        return semantic(Kind.TITLE, title);
    }

    /** Globally overrides the attribute used by {@link #byTestId(String)}. */
    public static void setTestIdAttribute(String attribute) {
        if (attribute != null && !attribute.isBlank()) {
            testIdAttribute = attribute.trim();
        }
    }

    private static Locator semantic(Kind kind, String value) {
        Locator l = new Locator(kind);
        l.semanticValue = value;
        return l;
    }

    private Locator(By root) {
        this.kind = Kind.CSS_OR_BY;
        this.root = root;
    }

    private Locator(Kind kind) {
        this.kind = kind;
        this.root = null;
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

    /**
     * Narrow a role-based locator to elements whose accessible name matches.
     * Case-insensitive substring by default; call {@link #exact()} for an exact match.
     * <pre>getByRole(Role.BUTTON).withName("Submit")</pre>
     */
    public Locator withName(String name) {
        this.accessibleName = name;
        return this;
    }

    /**
     * Narrow a {@link Role#HEADING} locator to a specific heading level (1–6).
     * <pre>getByRole(Role.HEADING).withLevel(1)</pre>
     */
    public Locator withLevel(int level) {
        this.headingLevel = level;
        return this;
    }

    /**
     * Switch text / name / attribute matching to exact (case-sensitive) instead of
     * the default case-insensitive substring match.
     * <pre>getByText("Submit").exact()</pre>
     */
    public Locator exact() {
        this.exact = true;
        return this;
    }

    /**
     * Returns the synthesized Selenium {@link By} for the base selector of this
     * locator — handy as an escape hatch into raw Selenium or {@code SmartLocator}.
     *
     * <p>Chain refinements that cannot be expressed as a {@code By}
     * (e.g. {@link #withName(String)}, {@link #filter(String)}) are not included;
     * use the terminal actions for the fully-resolved element.
     */
    public By toBy() {
        return buildRoot();
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
        By effectiveRoot = buildRoot();
        List<WebElement> candidates;

        if (withinContainer != null) {
            try {
                WebElement container = d.findElement(withinContainer);
                candidates = container.findElements(effectiveRoot);
            } catch (Exception e) {
                throw new LocatorException("Container not found: " + withinContainer + " — " + e.getMessage());
            }
        } else {
            candidates = d.findElements(effectiveRoot);
        }

        // apply accessible-name filter (role-based + label locators)
        if (accessibleName != null) {
            candidates = candidates.stream()
                    .filter(el -> nameMatches(d, el, accessibleName))
                    .collect(Collectors.toList());
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
    // Semantic selector synthesis
    // ------------------------------------------------------------------

    /** Builds the effective base {@link By} for this locator's {@link Kind}. */
    private By buildRoot() {
        switch (kind) {
            case CSS_OR_BY:
                return root;
            case ROLE: {
                String css = semanticRole.cssSelector();
                if (semanticRole == Role.HEADING && headingLevel != null) {
                    css = "h" + headingLevel
                            + ", [role='heading'][aria-level='" + headingLevel + "']";
                }
                return By.cssSelector(css);
            }
            case TEXT:
                return By.xpath(textXPath(semanticValue, exact));
            case LABEL:
                return By.cssSelector(FORM_CONTROL_CSS);
            case PLACEHOLDER:
                return By.cssSelector(attrCss("placeholder", semanticValue, exact));
            case ALT_TEXT:
                return By.cssSelector(attrCss("alt", semanticValue, exact));
            case TITLE:
                return By.cssSelector(attrCss("title", semanticValue, exact));
            case TESTID:
                return By.cssSelector("[" + testIdAttribute + "='" + cssEscape(semanticValue) + "']");
            default:
                throw new LocatorException("Unsupported locator kind: " + kind);
        }
    }

    /** CSS attribute selector — exact match, or case-insensitive substring by default. */
    private static String attrCss(String attr, String value, boolean exact) {
        return exact
                ? "[" + attr + "='" + cssEscape(value) + "']"
                : "[" + attr + "*='" + cssEscape(value) + "' i]";
    }

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";

    /** XPath matching visible text — normalized; exact equality or case-insensitive contains. */
    private static String textXPath(String value, boolean exact) {
        if (exact) {
            return ".//*[normalize-space(.)=" + xpathLiteral(value) + "]";
        }
        return ".//*[contains(translate(normalize-space(.),"
                + " '" + UPPER + "', '" + LOWER + "'),"
                + " " + xpathLiteral(value.toLowerCase()) + ")]";
    }

    /** Escapes a value for use inside a single-quoted CSS attribute selector. */
    private static String cssEscape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** Produces a safe XPath string literal, using concat() when both quote types appear. */
    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = value.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        return sb.append(")").toString();
    }

    /** True when an element's computed accessible name matches {@code expected}. */
    private boolean nameMatches(WebDriver d, WebElement el, String expected) {
        String actual;
        try {
            actual = (String) ((JavascriptExecutor) d).executeScript(ACCESSIBLE_NAME_JS, el);
        } catch (Exception e) {
            return false;
        }
        if (actual == null) return false;
        actual = actual.trim();
        if (exact) {
            return actual.equals(expected);
        }
        return actual.toLowerCase().contains(expected.toLowerCase());
    }

    /** Computes an element's accessible name, following the common ARIA precedence. */
    private static final String ACCESSIBLE_NAME_JS =
            "const e = arguments[0];"
            + "if(!e) return '';"
            + "const al = e.getAttribute && e.getAttribute('aria-label');"
            + "if(al && al.trim()) return al.trim();"
            + "const lb = e.getAttribute && e.getAttribute('aria-labelledby');"
            + "if(lb){const t = lb.split(/\\s+/).map(function(id){var r=document.getElementById(id);return r?r.textContent:'';}).join(' ').trim(); if(t) return t;}"
            + "if(e.labels && e.labels.length){return Array.prototype.map.call(e.labels,function(l){return l.textContent;}).join(' ').trim();}"
            + "const id = e.getAttribute && e.getAttribute('id');"
            + "if(id){try{var lab=document.querySelector('label[for=\"'+id+'\"]'); if(lab) return lab.textContent.trim();}catch(x){}}"
            + "const wrap = e.closest && e.closest('label'); if(wrap) return wrap.textContent.trim();"
            + "const txt = (e.textContent||'').trim(); if(txt) return txt;"
            + "const alt = e.getAttribute && e.getAttribute('alt'); if(alt && alt.trim()) return alt.trim();"
            + "const ti = e.getAttribute && e.getAttribute('title'); if(ti && ti.trim()) return ti.trim();"
            + "const val = e.value; if(val) return (''+val).trim();"
            + "const ph = e.getAttribute && e.getAttribute('placeholder'); if(ph && ph.trim()) return ph.trim();"
            + "return '';";

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
        StringBuilder sb = new StringBuilder(baseDescription());
        if (accessibleName  != null && kind == Kind.ROLE) {
            sb.append(".withName(\"").append(accessibleName).append("\")");
        }
        if (exact)                   sb.append(".exact()");
        if (withinContainer != null) sb.append(" within(").append(withinContainer).append(")");
        if (filterCss       != null) sb.append(".filter(\"").append(filterCss).append("\")");
        if (withText        != null) sb.append(".withText(\"").append(withText).append("\")");
        if (nthIndex        >= 0)    sb.append(".nth(").append(nthIndex).append(")");
        return sb.toString();
    }

    /** Friendly description of the base selector, mirroring the factory that built it. */
    private String baseDescription() {
        switch (kind) {
            case ROLE:
                String r = "getByRole(" + semanticRole.ariaName() + ")";
                return headingLevel != null ? r + ".withLevel(" + headingLevel + ")" : r;
            case TEXT:        return "getByText(\"" + semanticValue + "\")";
            case LABEL:       return "getByLabel(\"" + accessibleName + "\")";
            case PLACEHOLDER: return "getByPlaceholder(\"" + semanticValue + "\")";
            case TESTID:      return "getByTestId(\"" + semanticValue + "\")";
            case ALT_TEXT:    return "getByAltText(\"" + semanticValue + "\")";
            case TITLE:       return "getByTitle(\"" + semanticValue + "\")";
            case CSS_OR_BY:
            default:          return root.toString();
        }
    }

    @Override
    public String toString() {
        return "Locator[" + describe() + "]";
    }
}
