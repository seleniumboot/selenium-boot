package com.seleniumboot.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal resolution: turns a {@link Locator} chain into matched elements and
 * synthesizes the base selectors for the semantic locator kinds.
 * Package-private implementation detail of the locator package.
 */
final class LocatorResolution {

    private LocatorResolution() {
    }

    static WebElement resolve(Locator l) {
        List<WebElement> candidates = resolveAll(l);
        if (candidates.isEmpty()) {
            throw new LocatorException("No element found for: " + l.describe());
        }
        return candidates.get(0);
    }

    static List<WebElement> resolveAll(Locator l) {
        WebDriver d = l.driver();
        By effectiveRoot = buildRoot(l);
        List<WebElement> candidates;

        if (l.withinContainer != null) {
            try {
                WebElement container = d.findElement(l.withinContainer);
                candidates = container.findElements(effectiveRoot);
            } catch (Exception e) {
                throw new LocatorException("Container not found: " + l.withinContainer + " — " + e.getMessage());
            }
        } else {
            candidates = d.findElements(effectiveRoot);
        }

        // apply accessible-name filter (role-based + label locators)
        if (l.accessibleName != null) {
            candidates = candidates.stream()
                    .filter(el -> nameMatches(l, d, el, l.accessibleName))
                    .collect(Collectors.toList());
        }

        // apply filter
        if (l.filterCss != null) {
            String css = l.filterCss;
            candidates = candidates.stream()
                    .filter(el -> {
                        try { return !el.findElements(By.cssSelector("*")).isEmpty()
                                    || matchesCss(l, el, css); }
                        catch (Exception ignored) { return false; }
                    })
                    .collect(Collectors.toList());

            // simpler: keep elements that themselves match the extra css selector
            // re-query from parent if possible, else filter by attribute
            candidates = filterByCss(d, candidates, css);
        }

        // apply withText
        if (l.withText != null) {
            String target = l.withText;
            candidates = candidates.stream()
                    .filter(el -> {
                        try { return el.getText().trim().equals(target); }
                        catch (Exception ignored) { return false; }
                    })
                    .collect(Collectors.toList());
        }

        // apply nth / first / last
        if (l.last) {
            if (candidates.isEmpty()) {
                throw new LocatorException(
                        "last() requested but only 0 element(s) matched: " + l.describe());
            }
            List<WebElement> single = new ArrayList<>();
            single.add(candidates.get(candidates.size() - 1));
            return single;
        }

        if (l.nthIndex >= 0) {
            if (l.nthIndex >= candidates.size()) {
                String prefix = l.first ? "first()" : ("nth(" + l.nthIndex + ")");
                throw new LocatorException(
                        prefix + " requested but only " + candidates.size()
                        + " element(s) matched: " + l.describe());
            }
            List<WebElement> single = new ArrayList<>();
            single.add(candidates.get(l.nthIndex));
            return single;
        }

        return candidates;
    }

    /** Filter a candidate list to those whose outerHTML matches a CSS selector via JS. */
    static List<WebElement> filterByCss(WebDriver d, List<WebElement> candidates, String css) {
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

    static boolean matchesCss(Locator l, WebElement el, String css) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) l.driver();
            Object result = js.executeScript(
                    "return arguments[0].matches(arguments[1]);", el, css);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /** Builds the effective base {@link By} for this locator's {@link Locator.Kind}. */
    static By buildRoot(Locator l) {
        switch (l.kind) {
            case CSS_OR_BY:
                return l.root;
            case ROLE: {
                String css = l.semanticRole.cssSelector();
                if (l.semanticRole == Role.HEADING && l.headingLevel != null) {
                    css = "h" + l.headingLevel
                            + ", [role='heading'][aria-level='" + l.headingLevel + "']";
                }
                return By.cssSelector(css);
            }
            case TEXT:
                return By.xpath(textXPath(l.semanticValue, l.exact));
            case LABEL:
                return By.cssSelector(LocatorJs.FORM_CONTROL_CSS);
            case PLACEHOLDER:
                return By.cssSelector(attrCss("placeholder", l.semanticValue, l.exact));
            case ALT_TEXT:
                return By.cssSelector(attrCss("alt", l.semanticValue, l.exact));
            case TITLE:
                return By.cssSelector(attrCss("title", l.semanticValue, l.exact));
            case TESTID:
                return By.cssSelector("[" + Locator.testIdAttribute + "='" + cssEscape(l.semanticValue) + "']");
            default:
                throw new LocatorException("Unsupported locator kind: " + l.kind);
        }
    }

    /** CSS attribute selector — exact match, or case-insensitive substring by default. */
    static String attrCss(String attr, String value, boolean exact) {
        return exact
                ? "[" + attr + "='" + cssEscape(value) + "']"
                : "[" + attr + "*='" + cssEscape(value) + "' i]";
    }

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";

    /** XPath matching visible text — normalized; exact equality or case-insensitive contains. */
    static String textXPath(String value, boolean exact) {
        if (exact) {
            return ".//*[normalize-space(.)=" + xpathLiteral(value) + "]";
        }
        return ".//*[contains(translate(normalize-space(.),"
                + " '" + UPPER + "', '" + LOWER + "'),"
                + " " + xpathLiteral(value.toLowerCase()) + ")]";
    }

    /** Escapes a value for use inside a single-quoted CSS attribute selector. */
    static String cssEscape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** Produces a safe XPath string literal, using concat() when both quote types appear. */
    static String xpathLiteral(String value) {
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
    static boolean nameMatches(Locator l, WebDriver d, WebElement el, String expected) {
        String actual;
        try {
            actual = (String) ((JavascriptExecutor) d).executeScript(LocatorJs.ACCESSIBLE_NAME_JS, el);
        } catch (Exception e) {
            return false;
        }
        if (actual == null) return false;
        actual = actual.trim();
        if (l.exact) {
            return actual.equals(expected);
        }
        return actual.toLowerCase().contains(expected.toLowerCase());
    }
}
