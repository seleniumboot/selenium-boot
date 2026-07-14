---
description: "Migrate off WebDriverManager: modern Selenium bundles Selenium Manager, so Selenium Boot resolves driver binaries with zero WebDriverManager code — delete the setup calls and the dependency."
id: from-webdrivermanager
title: Migrate from WebDriverManager
sidebar_label: From WebDriverManager
sidebar_position: 2
---

# Migrate from WebDriverManager

If your project calls `WebDriverManager.chromedriver().setup()` (or Boni García's `io.github.bonigarcia:webdrivermanager` in general) to download browser drivers, you can **delete all of it**. Modern Selenium resolves driver binaries itself, and Selenium Boot uses that mechanism out of the box.

:::info Why WebDriverManager existed
Historically, Selenium needed a matching `chromedriver`/`geckodriver` binary on your machine, and keeping it in sync with the installed browser was painful. WebDriverManager solved that by downloading the right binary at runtime. Since **Selenium 4.6.0** (November 2022), that job moved *into* Selenium itself — see below.
:::

---

## Selenium Manager makes it built-in

Starting with **Selenium 4.6.0**, Selenium ships **Selenium Manager**: an automatic driver-resolution tool bundled with `selenium-java`. When no driver binary is found, Selenium detects your browser version, downloads the correct driver, and caches it — with **no code and no extra dependency**.

Selenium Boot is built on modern Selenium, so this is already active. You don't call it, configure it, or depend on it directly.

---

## Before / after

**Before** — WebDriverManager as a dependency plus a `setup()` call before every driver you create:

```xml title="pom.xml (remove this)"
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.x</version>
</dependency>
```

```java title="DriverFactory.java (remove the setup calls)"
import io.github.bonigarcia.wdm.WebDriverManager;

WebDriverManager.chromedriver().setup();
WebDriver driver = new ChromeDriver();

// Firefox
WebDriverManager.firefoxdriver().setup();
WebDriver driver = new FirefoxDriver();
```

**After** — nothing. There is no `setup()` call and no WebDriverManager dependency. With Selenium Boot you don't even create the driver — extend [`BaseTest`](/docs/guides/base-test) and the framework does it:

```java title="LoginTest.java"
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    public void loginTest() {
        open();  // driver already created — binary resolved automatically
        // ...
    }
}
```

Pick the browser in config, not code:

```yaml title="selenium-boot.yml"
browser:
  name: chrome   # chrome | firefox | edge | safari
```

---

## What gets deleted

| Before | After |
|---|---|
| `webdrivermanager` dependency in `pom.xml` / `build.gradle` | ✅ Removed — nothing replaces it |
| `WebDriverManager.chromedriver().setup()` calls | ✅ Selenium Manager (automatic) |
| Per-browser `.setup()` branches (Chrome/Firefox/Edge) | ✅ One `browser.name` config line |
| Pinning driver versions to match browsers | ✅ Selenium Manager matches them for you |

---

## FAQ

**Do I still need WebDriverManager for a specific browser version?**
No. Selenium Manager detects the installed browser and downloads a matching driver automatically, including for Edge and Firefox.

**What about offline / air-gapped CI?**
Selenium Manager caches drivers under `~/.cache/selenium`. Warm the cache once (or bake it into your CI image) and subsequent runs need no network. This is the same caching approach WebDriverManager used.

**Can I still point at a specific driver binary?**
Yes — Selenium honours the standard driver-path system properties (e.g. `webdriver.chrome.driver`) if you set one, so pre-provisioned binaries keep working. You just no longer *need* to.

---

## Next steps

- [Migrate from Selenium + TestNG](/docs/migration/from-selenium-testng) — the full framework migration (driver factory, waits, retry, reporting)
- [Getting Started](/docs/getting-started) — the 5-minute version
- [Configuration Reference](/docs/configuration) — the full `selenium-boot.yml`
