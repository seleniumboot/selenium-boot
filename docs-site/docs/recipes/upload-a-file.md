---
description: "Upload a file in Selenium without OS dialogs: Selenium Boot's upload(By, path) sets the file input directly, with paths resolved from the classpath or an absolute location."
id: upload-a-file
title: Upload a file
sidebar_label: Upload a file
---

# Upload a file

Set a file `<input type="file">` directly — no brittle OS file-picker automation. `BasePage` exposes an `upload(By, path)` helper:

```java title="UploadPage.java"
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;

public class UploadPage extends BasePage {

    public void attachResume(String path) {
        upload(By.id("file-input"), path);   // sets the input's value directly
        click(By.id("submit"));
    }
}
```

```java
// From your test:
new UploadPage().attachResume("testfiles/resume.pdf");  // classpath-relative
new UploadPage().attachResume("/absolute/path/to/photo.png");
```

- The path may be **classpath-relative** (e.g. `testfiles/resume.pdf`) or **absolute**. Selenium Boot resolves it and fails fast with a clear error if the file doesn't exist.
- Works with hidden/off-screen inputs — it sets the input value, so no visible dialog is required.
- For a **multiple** upload, pass separated paths to the input as your app expects, or call the helper per file.

:::tip
Uploads go through a page object because `upload(...)` is a `BasePage` helper. Keep the file under `src/test/resources` so the classpath-relative path works on any machine and in CI.
:::

**Deeper reference:** [BasePage](/docs/guides/base-page) — the base class for page objects and its helpers.
