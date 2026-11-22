package com.seleniumboot.locator;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
    enum Kind { CSS_OR_BY, ROLE, TEXT, LABEL, PLACEHOLDER, TESTID, ALT_TEXT, TITLE }

    /** Default attribute used by {@link #byTestId(String)} — overridable. */
    static volatile String testIdAttribute = "data-testid";

    final Kind kind;
    final By root;            // set only for Kind.CSS_OR_BY
    Role semanticRole;        // set only for Kind.ROLE
    String semanticValue;     // raw text/attr value for the semantic kinds
    Integer headingLevel;     // optional heading level filter
    String accessibleName;    // withName(...) filter for role/label kinds
    boolean exact;            // exact (case-sensitive) vs substring (case-insensitive)

    String filterCss;
    String withText;
    By withinContainer;
    int nthIndex = -1;   // -1 = no nth filter
    boolean first;
    boolean last;

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
        this.first = false;
        this.last = false;
        this.nthIndex = index;
        return this;
    }

    /**
     * Narrow results to the first matched element.
     *
     * <p>Equivalent to {@link #nth(int) nth(0)}.
     *
     * <pre>$(".item").first().click();</pre>
     *
     * @return this locator for chaining
     */
    public Locator first() {
        this.first = true;
        this.last = false;
        this.nthIndex = 0;
        return this;
    }

    /**
     * Narrow results to the last matched element.
     *
     * <p>Resolves lazily at action time on every poll, ensuring dynamic element lists
     * and auto-waiting reflect the current DOM state when the terminal action runs.
     *
     * <pre>$(".item").last().click();</pre>
     *
     * @return this locator for chaining
     */
    public Locator last() {
        this.last = true;
        this.first = false;
        this.nthIndex = -1;
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
        return LocatorResolution.buildRoot(this);
    }

    // ------------------------------------------------------------------
    // Terminal actions — all auto-wait. Bodies live in the package-private
    // helpers LocatorActions / LocatorQueries / LocatorWaits; every public
    // method below keeps its exact signature and delegates unchanged.
    // ------------------------------------------------------------------

    /**
     * Waits for the element to reach the specified {@link State}.
     *
     * <p>Re-resolves from scratch on every poll and retries stale or not-yet-present
     * elements until {@code timeouts.explicit} elapses.
     *
     * <pre>
     * $("#spinner").waitFor(State.DETACHED);
     * $(".toast").waitFor(State.VISIBLE);
     * </pre>
     *
     * @param state target element lifecycle state (visible, hidden, attached, detached)
     * @throws LocatorException if the element does not reach {@code state} within {@code timeouts.explicit}
     */
    public void waitFor(State state) {
        LocatorWaits.waitFor(this, state);
    }

    /**
     * Waits for the element to be visible.
     *
     * <p>Equivalent to {@code waitFor(State.VISIBLE)}.
     *
     * @throws LocatorException if the element does not become visible within {@code timeouts.explicit}
     */
    public void waitFor() {
        waitFor(State.VISIBLE);
    }

    /**
     * Waits for the element to exist and be clickable, then clicks it.
     *
     * <p>Like every terminal action, the element is re-resolved on each poll: one that is not in
     * the DOM yet is waited for, and one the page re-renders between lookup and action does not
     * surface as a {@link org.openqa.selenium.StaleElementReferenceException}. Both are bounded by
     * {@code timeouts.explicit}.
     */
    public void click() {
        LocatorActions.click(this);
    }

    /**
     * Waits for the element to be visible and enabled, then double-clicks it.
     *
     * <p>Re-resolves on every poll and retries stale or not-yet-present elements
     * until {@code timeouts.explicit} elapses, including staleness during the gesture.
     *
     * <pre>getByText("Edit item").doubleClick();</pre>
     *
     * @throws LocatorException if no element matches within {@code timeouts.explicit}
     * @throws TimeoutException if a matching element never becomes ready or remains stale
     */
    public void doubleClick() {
        LocatorActions.doubleClick(this);
    }

    /**
     * Waits for the element to be visible, then right-clicks it (context click).
     *
     * <p>Re-resolves on every poll and retries stale or not-yet-present elements
     * until {@code timeouts.explicit} elapses, including staleness during the gesture.
     *
     * <pre>getByText("Options").rightClick();</pre>
     *
     * @throws LocatorException if no element matches within {@code timeouts.explicit}
     * @throws TimeoutException if a matching element never becomes visible or remains stale
     */
    public void rightClick() {
        LocatorActions.rightClick(this);
    }

    /**
     * Waits for the element to be visible and enabled, then checks it if it is not already checked.
     *
     * <p>Idempotent: does not click if the element is already checked. Works on checkboxes,
     * radio buttons, and elements with {@code role="checkbox"}. Re-resolves on every poll
     * and retries stale or not-yet-present elements until {@code timeouts.explicit} elapses.
     */
    public void check() {
        LocatorActions.check(this);
    }

    /**
     * Waits for the element to be visible and enabled, then unchecks it if it is currently checked.
     *
     * <p>Idempotent: does not click if the element is already unchecked. Works on checkboxes
     * and elements with {@code role="checkbox"}. Re-resolves on every poll and retries stale
     * or not-yet-present elements until {@code timeouts.explicit} elapses.
     */
    public void uncheck() {
        LocatorActions.uncheck(this);
    }

    /** Waits for the element to be visible, clears it, then types the given text. */
    public void type(String text) {
        LocatorActions.type(this, text);
    }

    /** Appends text without clearing first. */
    public void append(String text) {
        LocatorActions.append(this, text);
    }

    /**
     * Waits for the element to be visible, then sends the given keys without clearing its value.
     *
     * <p>Keys are sent in order using {@link WebElement#sendKeys(CharSequence...)}.
     * Re-resolves on every poll and retries stale or not-yet-present elements
     * until {@code timeouts.explicit} elapses, including staleness while sending keys.
     *
     * <pre>getByLabel("Search").press(Keys.ENTER);</pre>
     *
     * @param keys Selenium keys to send to the element
     * @throws LocatorException if no element matches within {@code timeouts.explicit}
     * @throws TimeoutException if a matching element never becomes visible or remains stale
     */
    public void press(Keys... keys) {
        LocatorActions.press(this, keys);
    }

    /**
     * Waits for the element to be visible, then focuses it using JavaScript without clicking it.
     *
     * <p>The target must be focusable (for example, an input or an element with {@code tabindex}).
     * Re-resolves on every poll and retries stale or not-yet-present elements
     * until {@code timeouts.explicit} elapses, including staleness while focusing.
     *
     * <pre>getByLabel("Search").focus();</pre>
     *
     * @throws LocatorException if no element matches within {@code timeouts.explicit}
     * @throws TimeoutException if a matching element never becomes visible or remains stale
     */
    public void focus() {
        LocatorActions.focus(this);
    }

    /**
     * Waits for the element to be visible and clears its text or value.
     *
     * <p>Re-resolves on every poll and retries stale or not-yet-present elements
     * until {@code timeouts.explicit} elapses.
     */
    public void clear() {
        LocatorActions.clear(this);
    }

    /** Waits for the element to be visible and returns its trimmed visible text. */
    public String getText() {
        return LocatorQueries.getText(this);
    }

    /** Returns the value of the given attribute, waiting for visibility first. */
    public String getAttribute(String name) {
        return LocatorQueries.getAttribute(this, name);
    }

    /**
     * Waits for the element to be visible and returns the current value of an {@code <input>},
     * {@code <textarea>}, or other form control.
     *
     * <p>Retrieves the element's DOM {@code value} property, falling back to the {@code value}
     * attribute. Returns an empty string if no value is present.
     *
     * @return the input's current value, or {@code ""} if none
     */
    public String inputValue() {
        return LocatorQueries.inputValue(this);
    }

    /** Returns true if the element is present and displayed — does NOT wait. */
    public boolean isVisible() {
        return LocatorQueries.isVisible(this);
    }

    /** Returns true if the element is absent or not displayed — does NOT wait. */
    public boolean isHidden() {
        return LocatorQueries.isHidden(this);
    }

    /** Returns true if the element is present and enabled. */
    public boolean isEnabled() {
        return LocatorQueries.isEnabled(this);
    }

    /**
     * Returns true if the element (checkbox, radio button, or ARIA checkable control)
     * is present and checked — does NOT wait.
     */
    public boolean isChecked() {
        return LocatorQueries.isChecked(this);
    }

    /** Hovers over the element using Actions. */
    public void hover() {
        LocatorActions.hover(this);
    }

    /** Scrolls the element into view using JavaScript. */
    public void scrollIntoView() {
        LocatorActions.scrollIntoView(this);
    }

    /** Clicks using JavaScript — useful when element is obscured. */
    public void jsClick() {
        LocatorActions.jsClick(this);
    }

    /**
     * Drags this element onto {@code target} and drops it there.
     *
     * <p>Tries the native {@link org.openqa.selenium.interactions.Actions#dragAndDrop} gesture first. Pages built on the HTML5
     * drag-and-drop API ({@code draggable="true"} plus {@code dragstart}/{@code drop} listeners)
     * often ignore it, so when no {@code drop} event reached the target, the full sequence
     * ({@code dragstart, dragenter, dragover, drop, dragend}) is dispatched with a synthetic
     * {@code DataTransfer}. Both elements are waited for and re-resolved like every other
     * terminal action.
     *
     * <pre>{@code
     * Locator.byText("Card A").dragTo(Locator.ofCss("#done-column"));
     * }</pre>
     *
     * @throws LocatorException if either element cannot be resolved within {@code timeouts.explicit}
     */
    public void dragTo(Locator target) {
        LocatorActions.dragTo(this, target);
    }

    /**
     * Selects an option in a {@code <select>} element by its visible text.
     *
     * <p>Waits for the element to be visible, and re-resolves from scratch on every poll
     * until {@code timeouts.explicit} elapses.
     *
     * @param text visible text of the option to select
     */
    public void selectOption(String text) {
        LocatorActions.selectOption(this, text);
    }

    /**
     * Selects an option in a {@code <select>} element by its {@code value} attribute.
     *
     * <p>Waits for the element to be visible, and re-resolves from scratch on every poll
     * until {@code timeouts.explicit} elapses.
     *
     * @param value value attribute of the option to select
     */
    public void selectOptionByValue(String value) {
        LocatorActions.selectOptionByValue(this, value);
    }

    /**
     * Captures a PNG screenshot of the resolved element and returns its bytes.
     *
     * <p>Waits for the element to be visible, re-resolving from scratch on every poll
     * and retrying stale or not-yet-present elements until {@code timeouts.explicit} elapses.
     *
     * @return PNG image bytes of the element
     * @throws LocatorException if the element does not become visible within {@code timeouts.explicit}
     */
    public byte[] screenshot() {
        return LocatorQueries.screenshot(this);
    }

    /**
     * Captures a PNG screenshot of the resolved element and writes it to the specified file path.
     *
     * <p>Creates parent directories if they do not exist. Waits for the element to be visible,
     * re-resolving from scratch on every poll until {@code timeouts.explicit} elapses.
     *
     * @param path destination path for the PNG file
     * @throws LocatorException if the element cannot be resolved or if writing to the path fails
     */
    public void screenshot(Path path) {
        LocatorQueries.screenshot(this, path);
    }

    /** Returns the number of elements currently matching the locator chain (no wait). */
    public int count() {
        return LocatorQueries.count(this);
    }

    /**
     * Returns the text content of all elements matching this locator chain, without waiting for
     * elements to appear.
     *
     * <p>Returns an empty list immediately if no elements match. Returns each element's rendered
     * text ({@link WebElement#getText()}), falling back to its DOM {@code textContent} property
     * for hidden elements whose rendered text is blank. If an element goes stale while its text
     * is being read, the list is re-resolved from scratch until {@code timeouts.explicit} elapses.
     *
     * @return list of text contents of all matched elements
     */
    public List<String> allTextContents() {
        return LocatorQueries.allTextContents(this);
    }

    /** Returns the resolved {@link WebElement}, applying all chain filters. */
    public WebElement element() {
        return LocatorQueries.element(this);
    }

    /** Returns all matched {@link WebElement}s, applying all chain filters. */
    public List<WebElement> elements() {
        return LocatorQueries.elements(this);
    }

    /**
     * Resolves this locator to a {@code <table>} and returns its data rows as
     * header-keyed maps — one map per row, keys in column order.
     *
     * <p>Headers are taken from the first {@code <thead>} row that has
     * {@code <th>} cells; failing that, the first {@code <tr>} anywhere in the
     * table that has {@code <th>} cells; failing that, the {@code <td>} cells of
     * the first {@code <tr>}. Whichever row supplies the headers is never
     * returned as data.
     *
     * <p>All text is trimmed. A row with fewer {@code <td>} cells than there are
     * headers is padded with {@code ""}; cells beyond the last header are
     * dropped. Rows with no {@code <td>} at all — {@code <th>}-only group
     * headings, for instance — are skipped. The result is empty when the table
     * has no rows, or when no headers could be derived.
     *
     * <p>Like every terminal action this waits for the table to be visible, and
     * throws {@link LocatorException} if it never matches. Rows and cells are
     * matched by descendant search, so a nested table's rows are read as part of
     * the outer table.
     *
     * <pre>
     * List&lt;Map&lt;String, String&gt;&gt; data = $("table#results").rows();
     * assertEquals(data.get(0).get("Status"), "Active");
     * </pre>
     */
    public List<Map<String, String>> rows() {
        return LocatorQueries.rows(this);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    static boolean isSelected(WebElement el) {
        return el.isSelected() || "true".equalsIgnoreCase(el.getAttribute("aria-checked"));
    }

    WebDriver driver() {
        return DriverManager.getDriver();
    }

    String describe() {
        StringBuilder sb = new StringBuilder(baseDescription());
        if (accessibleName  != null && kind == Kind.ROLE) {
            sb.append(".withName(\"").append(accessibleName).append("\")");
        }
        if (exact)                   sb.append(".exact()");
        if (withinContainer != null) sb.append(" within(").append(withinContainer).append(")");
        if (filterCss       != null) sb.append(".filter(\"").append(filterCss).append("\")");
        if (withText        != null) sb.append(".withText(\"").append(withText).append("\")");
        if (last)                    sb.append(".last()");
        else if (first)              sb.append(".first()");
        else if (nthIndex   >= 0)    sb.append(".nth(").append(nthIndex).append(")");
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
