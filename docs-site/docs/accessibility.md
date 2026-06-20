---
id: accessibility
title: Accessibility Assertions
sidebar_position: 17
---

# Accessibility Assertions (axe-core)

Selenium Boot 2.5.0 bundles [axe-core](https://github.com/dequelabs/axe-core) 4.10.2 directly in the JAR. Call `accessibility()` after `open()` to run a WCAG scan with zero extra dependencies.

---

## Quick example

```java
public class CheckoutAccessibilityTest extends BaseTest {

    @Test
    public void checkout_passesWCAG21AA() {
        open("/checkout");

        accessibility()
            .withTags("wcag2a", "wcag21aa")   // WCAG 2.1 AA rules only
            .withLevel(Impact.SERIOUS)         // fail on SERIOUS or CRITICAL
            .excluding("#cookie-banner")       // skip third-party widget
            .run();
    }
}
```

---

## API reference

### `accessibility()` — entry point

Available in `BaseTest` and `BaseJUnit5Test`. Returns an `AccessibilityAssert` builder.

### Builder methods

| Method | Description |
|---|---|
| `.withTags(String... tags)` | Run only rules matching the given axe-core tags. Common: `"wcag2a"`, `"wcag2aa"`, `"wcag21aa"`, `"wcag22aa"`, `"best-practice"`. Default: all rules. |
| `.withLevel(Impact minimum)` | Fail only on violations at or above this severity. Default: `Impact.MINOR` (all violations fail). |
| `.excluding(String... selectors)` | CSS selectors to exclude from the scan. Subtrees rooted at these elements are ignored. |
| `.withContext(String selector)` | Restrict the scan to the subtree rooted at this CSS selector. |

### Terminal methods

| Method | Description |
|---|---|
| `.run()` | Execute scan and throw `AssertionError` if violations are found at the configured level. |
| `.collect()` | Execute scan and return `AccessibilityResult` without asserting. |

---

## Impact levels

```java
Impact.CRITICAL   // inaccessible to assistive technology users
Impact.SERIOUS    // severe barrier with possible workarounds
Impact.MODERATE   // degraded experience without complete blocker
Impact.MINOR      // best-practice deviation with low real-world impact
```

Ordering: `CRITICAL > SERIOUS > MODERATE > MINOR`.

---

## Scoped scans

```java
// Scan only the login form
accessibility()
    .withContext("#login-form")
    .run();

// Exclude known inaccessible third-party widgets
accessibility()
    .excluding("#intercom-container", "#zendesk-widget")
    .run();
```

---

## Collecting results without asserting

```java
AccessibilityResult result = accessibility()
    .withTags("wcag2a", "wcag21aa")
    .collect();

System.out.println("Violations: " + result.violationCount());
System.out.println("Passed rules: " + result.passCount());

for (AccessibilityViolation v : result.violations()) {
    System.out.println("[" + v.impact() + "] " + v.id() + " — " + v.help());
    v.nodes().forEach(n -> System.out.println("  " + n.target()));
}

// Soft assertion — tolerate up to 2 minor violations
Assert.assertTrue(
    result.violationsAtLevel(Impact.SERIOUS).isEmpty(),
    "Serious violations found:\n" + result.violations()
);
```

---

## Error message format

When `.run()` finds violations it throws an `AssertionError` with a full report:

```
[Accessibility] 2 violations found on: https://example.com/checkout
  Rules: wcag2a, wcag21aa
  Minimum impact: SERIOUS

  1. [CRITICAL] image-alt
     Ensures <img> elements have alternate text
     Fix: Images must have alternate text
     Docs: https://dequeuniversity.com/rules/axe/4.10/image-alt
     → img.hero-banner
       Fix any of the following: Element does not have an alt attribute

  2. [SERIOUS] color-contrast
     Ensures the contrast ratio between foreground and background colors meets thresholds
     Fix: Elements must have sufficient color contrast
     Docs: https://dequeuniversity.com/rules/axe/4.10/color-contrast
     → p.disclaimer-text
       Fix any of the following: Element has insufficient color contrast of 3.5 (Expected 4.5:1)
     … and 3 more node(s)
```

---

## Common WCAG tag combinations

| Goal | Tags |
|---|---|
| WCAG 2.0 A | `"wcag2a"` |
| WCAG 2.0 AA (minimum legal requirement in many countries) | `"wcag2a"`, `"wcag2aa"` |
| WCAG 2.1 AA (EU Accessibility Act, ADA) | `"wcag2a"`, `"wcag21aa"` |
| WCAG 2.2 AA | `"wcag2a"`, `"wcag21aa"`, `"wcag22aa"` |
| Best practices (beyond WCAG) | `"best-practice"` |

---

## Notes

- axe-core is injected into the browser once per page load. Subsequent `accessibility()` calls on the same page reuse the already-injected instance.
- No internet connection is required — axe-core 4.10.2 is bundled in the JAR as a classpath resource.
- No extra Maven dependency is needed.
