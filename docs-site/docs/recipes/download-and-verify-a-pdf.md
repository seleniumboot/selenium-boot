---
description: "Download and verify a PDF in Selenium: DownloadManager waits for the file to appear in the download directory, handling partial downloads, so you can assert it exists and is non-empty."
id: download-and-verify-a-pdf
title: Download and verify a PDF
sidebar_label: Download & verify a PDF
---

# Download and verify a PDF

Click a download link, wait for the file to land, and assert it's a real PDF. `DownloadManager` polls the download directory and ignores partial (`.crdownload`) files until they're complete — no `Thread.sleep()`.

```java title="InvoiceTest.java"
import com.seleniumboot.browser.DownloadManager;
import com.seleniumboot.test.BaseTest;
import java.io.File;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InvoiceTest extends BaseTest {

    @Test
    public void downloadsInvoicePdf() {
        open("/invoices/123");
        $("#download-pdf").click();

        // Dynamic filename (timestamped) → wait for any file, up to 15s:
        File pdf = DownloadManager.waitForAnyFile(15);

        Assert.assertTrue(pdf.getName().endsWith(".pdf"));
        Assert.assertTrue(pdf.length() > 0);   // non-empty
    }
}
```

Known filename? Wait for it by name instead:

```java
File pdf = DownloadManager.waitForFile("invoice-123.pdf", 15);
```

Set the download directory in config (defaults to `./target/downloads`):

```yaml title="selenium-boot.yml"
browser:
  downloadDir: ./target/downloads
```

:::tip Clean state between tests
Call `DownloadManager.clearDownloads()` in `@BeforeMethod` so a stale file from a previous run can't satisfy the wait.
:::

**Deeper reference:** [DownloadManager](/docs/guides/download-manager) — waiting strategies, partial-download handling, and cleanup.
