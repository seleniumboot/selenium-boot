---
id: custom-drivers
title: Custom Drivers
sidebar_position: 1
---

# Custom Drivers

Selenium Boot provisions browsers automatically using WebDriverManager. For advanced scenarios — remote grids, cloud providers, custom capabilities — you can take full control of driver creation.

---

## Remote WebDriver (Selenium Grid)

Point your tests at a Selenium Grid hub:

```yaml title="selenium-boot.yml"
browser:
  type: chrome
  remote: true
  remoteUrl: http://selenium-hub:4444/wd/hub
```

Selenium Boot creates a `RemoteWebDriver` with the configured browser type's capabilities.

---

## Cloud providers

### BrowserStack

```yaml
browser:
  type: chrome
  remote: true
  remoteUrl: https://hub-cloud.browserstack.com/wd/hub
  capabilities:
    bstack:options:
      os: Windows
      osVersion: 11
      browserName: Chrome
      browserVersion: latest
      userName: ${BROWSERSTACK_USERNAME}
      accessKey: ${BROWSERSTACK_ACCESS_KEY}
```

### Sauce Labs

```yaml
browser:
  type: chrome
  remote: true
  remoteUrl: https://ondemand.saucelabs.com:443/wd/hub
  capabilities:
    sauce:options:
      username: ${SAUCE_USERNAME}
      accessKey: ${SAUCE_ACCESS_KEY}
```

---

## Custom Chrome options

Pass arbitrary Chrome arguments and preferences:

```yaml
browser:
  type: chrome
  headless: true
  arguments:
    - --disable-extensions
    - --no-sandbox
    - --disable-dev-shm-usage
    - --window-size=1920,1080
```

---

## Mobile emulation

```yaml
browser:
  type: chrome
  mobileEmulation:
    deviceName: iPhone 12 Pro
```

---

## Firefox profile

```yaml
browser:
  type: firefox
  profilePath: /path/to/firefox-profile
```

---

## Using `getDriver()` directly

For anything not covered by configuration, you always have full access to the underlying WebDriver:

```java
public class MyTest extends BaseTest {

    @Test
    public void customCapabilityTest() {
        WebDriver driver = getDriver();
        // driver is fully configured — cast to ChromeDriver, RemoteWebDriver, etc.
        ((ChromeDriver) driver).executeCdpCommand("...", Map.of());
    }
}
```
