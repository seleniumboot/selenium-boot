---
id: screenshots
title: Screenshots
sidebar_position: 8
---

# Screenshots

Selenium Boot captures screenshots automatically on test failure and embeds them directly in the HTML report as Base64 — no external image files, no broken paths in CI.

---

## Automatic failure screenshots

No configuration needed. When a test fails, Selenium Boot captures a screenshot and attaches it to the test's detail panel in the HTML report.

The screenshot appears as a thumbnail in the Failures tab. Click it to open the full-size lightbox.

---

## Step screenshots

Capture a screenshot at any point during a test using `StepLogger`:

```java
StepLogger.step("After form submission", true);        // INFO + screenshot
StepLogger.step("Verify result", StepStatus.PASS, true); // PASS + screenshot
```

Step screenshots appear as thumbnails in the step timeline inside each test's detail panel.

---

## How screenshots are stored

Screenshots are encoded as Base64 and embedded directly in the HTML report. This means:

- The report is a **single self-contained file** — copy it anywhere
- Works in **CI environments** where the workspace is cleaned after the run
- No path configuration required
- File size is larger than a separate image file (Base64 adds ~33% overhead)

---

## Configuration

```yaml title="selenium-boot.yml"
screenshots:
  onFailure: true   # default — capture screenshot on every failure
```

To disable automatic failure screenshots:

```yaml
screenshots:
  onFailure: false
```

Step screenshots (via `StepLogger`) are controlled by the `boolean screenshot` argument in the method call, not by this config.

---

## Custom screenshot capture

If you need to capture a screenshot outside of `StepLogger` (for example, in a utility method), use `ScreenshotManager` directly:

```java
import com.seleniumboot.reporting.ScreenshotManager;

// Capture and save to disk (returns file path)
String path = ScreenshotManager.capture(driver, "my-screenshot");

// Capture as Base64 string (for embedding)
String base64 = ScreenshotManager.captureAsBase64(driver);
```

---

## Screenshot in CI

Because screenshots are Base64-embedded, they appear correctly in the HTML report artifact even after the CI workspace is deleted.

**GitHub Actions** — upload the report as an artifact:

```yaml
- name: Upload report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: selenium-boot-report
    path: target/selenium-boot-report.html
```

Download the artifact, open the HTML file locally — all screenshots are inline.
