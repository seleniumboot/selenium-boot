package com.seleniumboot.locator;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * WAI-ARIA roles used by the accessibility-first locators
 * ({@code getByRole(...)}).
 *
 * <p>Each role maps to a CSS selector that matches both the elements with an
 * explicit {@code role="…"} attribute <em>and</em> the native HTML elements that
 * carry that role implicitly (e.g. {@code <button>} and {@code <input
 * type="submit">} both have the implicit role {@code button}).
 *
 * <p>This mirrors Playwright's {@code getByRole()} — locators target the
 * accessibility tree the user actually perceives, so they survive CSS and DOM
 * refactors that break brittle structural selectors.
 *
 * <pre>
 * getByRole(Role.BUTTON).withName("Submit").click();
 * getByRole(Role.HEADING).withLevel(1).getText();
 * getByRole(Role.LINK, "Forgot password?").click();
 * </pre>
 *
 * @since 3.1.0
 */
@SeleniumBootApi(since = "3.1.0")
public enum Role {

    BUTTON("button",
            "button, [role='button'], input[type='button'], input[type='submit'], "
            + "input[type='reset'], input[type='image'], summary"),

    LINK("link", "a[href], area[href], [role='link']"),

    CHECKBOX("checkbox", "input[type='checkbox'], [role='checkbox']"),

    RADIO("radio", "input[type='radio'], [role='radio']"),

    SWITCH("switch", "[role='switch']"),

    TEXTBOX("textbox",
            "input:not([type]), input[type='text'], input[type='email'], "
            + "input[type='password'], input[type='tel'], input[type='url'], "
            + "input[type='number'], textarea, [role='textbox'], [contenteditable='true']"),

    SEARCHBOX("searchbox", "input[type='search'], [role='searchbox']"),

    COMBOBOX("combobox", "select, [role='combobox']"),

    OPTION("option", "option, [role='option']"),

    HEADING("heading", "h1, h2, h3, h4, h5, h6, [role='heading']"),

    IMG("img", "img, [role='img']"),

    LIST("list", "ul, ol, [role='list']"),

    LISTITEM("listitem", "li, [role='listitem']"),

    TABLE("table", "table, [role='table']"),

    ROW("row", "tr, [role='row']"),

    CELL("cell", "td, [role='cell'], [role='gridcell']"),

    COLUMNHEADER("columnheader", "th, [role='columnheader']"),

    NAVIGATION("navigation", "nav, [role='navigation']"),

    BANNER("banner", "header, [role='banner']"),

    CONTENTINFO("contentinfo", "footer, [role='contentinfo']"),

    MAIN("main", "main, [role='main']"),

    REGION("region", "section, [role='region']"),

    ARTICLE("article", "article, [role='article']"),

    DIALOG("dialog", "dialog, [role='dialog']"),

    ALERT("alert", "[role='alert']"),

    ALERTDIALOG("alertdialog", "[role='alertdialog']"),

    TAB("tab", "[role='tab']"),

    TABLIST("tablist", "[role='tablist']"),

    TABPANEL("tabpanel", "[role='tabpanel']"),

    MENU("menu", "menu, [role='menu']"),

    MENUITEM("menuitem", "[role='menuitem']"),

    PROGRESSBAR("progressbar", "progress, [role='progressbar']"),

    SPINBUTTON("spinbutton", "input[type='number'], [role='spinbutton']"),

    SLIDER("slider", "input[type='range'], [role='slider']"),

    PARAGRAPH("paragraph", "p, [role='paragraph']"),

    SEPARATOR("separator", "hr, [role='separator']");

    private final String ariaName;
    private final String cssSelector;

    Role(String ariaName, String cssSelector) {
        this.ariaName = ariaName;
        this.cssSelector = cssSelector;
    }

    /** The ARIA role token (e.g. {@code "button"}). */
    public String ariaName() {
        return ariaName;
    }

    /** CSS selector matching every element that has this role, implicitly or explicitly. */
    public String cssSelector() {
        return cssSelector;
    }

    /** True for roles whose accessible name comes from descendant text content. */
    boolean nameFromContent() {
        switch (this) {
            case TEXTBOX:
            case SEARCHBOX:
            case COMBOBOX:
            case SPINBUTTON:
            case SLIDER:
                return false;   // name comes from label / value, not content
            default:
                return true;
        }
    }
}
