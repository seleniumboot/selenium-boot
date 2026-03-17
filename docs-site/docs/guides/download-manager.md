---
id: download-manager
title: DownloadManager
sidebar_position: 10
---

# DownloadManager

`DownloadManager` verifies file downloads during tests. It polls the configured download directory and waits for a file to appear, handling partial downloads gracefully.

---

## Configuration

```yaml title="selenium-boot.yml"
browser:
  downloadDir: ./target/downloads   # default
```

---

## Wait for a specific file

```java
import com.seleniumboot.browser.DownloadManager;

@Test
public void exportCsvTest() {
    click(By.id("export-csv"));

    File downloaded = DownloadManager.waitForFile("report.csv", 15);
    Assert.assertTrue(downloaded.exists());
    Assert.assertTrue(downloaded.length() > 0);
}
```

---

## Wait for any file

When the filename is dynamic (e.g. contains a timestamp):

```java
click(By.id("download-invoice"));

File invoice = DownloadManager.waitForAnyFile(15);
Assert.assertTrue(invoice.getName().endsWith(".pdf"));
```

---

## Clean up between tests

```java
@BeforeMethod
public void cleanDownloads() {
    DownloadManager.clearDownloads();
}
```

---

## How partial downloads are handled

`DownloadManager` ignores files with these extensions until they are complete:

| Extension | Browser |
|---|---|
| `.crdownload` | Chrome |
| `.part` | Firefox |

A file is only returned once it exists, has a non-zero size, and has no partial-download extension.

---

## Get the download directory

```java
File dir = DownloadManager.resolveDownloadDir();
System.out.println("Downloads at: " + dir.getAbsolutePath());
```

The directory is created automatically if it does not exist.
