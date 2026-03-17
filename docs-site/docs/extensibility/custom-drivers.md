---
id: custom-drivers
title: Custom Drivers
sidebar_position: 3
---

# Custom Drivers

`NamedDriverProvider` lets you plug in any WebDriver implementation — Edge, Safari, BrowserStack, Appium — without modifying the framework. Custom providers take precedence over the built-in Chrome/Firefox providers.

---

## Create a custom driver provider

```java
import com.seleniumboot.driver.NamedDriverProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public class EdgeDriverProvider implements NamedDriverProvider {

    @Override
    public String browserName() {
        return "edge";   // matched case-insensitively against browser.name in selenium-boot.yml
    }

    @Override
    public WebDriver createDriver() {
        EdgeOptions options = new EdgeOptions();
        return new EdgeDriver(options);
    }
}
```

---

## Register via Java SPI (auto-discovery)

```
src/main/resources/META-INF/services/com.seleniumboot.driver.NamedDriverProvider
```

Contents:

```
com.example.drivers.EdgeDriverProvider
```

Then set `browser.name` in your config:

```yaml title="selenium-boot.yml"
browser:
  name: edge
```

Selenium Boot selects your provider automatically.

---

## Register programmatically

```java
import com.seleniumboot.driver.DriverProviderRegistry;

DriverProviderRegistry.register(new EdgeDriverProvider());
```

---

## BrowserStack example

```java
public class BrowserStackProvider implements NamedDriverProvider {

    @Override
    public String browserName() { return "browserstack"; }

    @Override
    public WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        HashMap<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName", System.getenv("BROWSERSTACK_USERNAME"));
        bstackOptions.put("accessKey", System.getenv("BROWSERSTACK_ACCESS_KEY"));
        bstackOptions.put("browserName", "Chrome");
        bstackOptions.put("browserVersion", "latest");
        options.setCapability("bstack:options", bstackOptions);

        return new RemoteWebDriver(
            new URL("https://hub-cloud.browserstack.com/wd/hub"), options
        );
    }
}
```

```yaml title="selenium-boot.yml"
browser:
  name: browserstack
```

---

## Appium example

```java
public class AndroidAppProvider implements NamedDriverProvider {

    @Override
    public String browserName() { return "android"; }

    @Override
    public WebDriver createDriver() {
        UiAutomator2Options options = new UiAutomator2Options()
            .setDeviceName("emulator-5554")
            .setApp("/path/to/app.apk");

        return new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    }
}
```

---

## Provider selection order

1. **Remote mode** (`browser.mode: remote`) → always uses `RemoteDriverProvider`
2. **Custom provider** registered via SPI or programmatically → used if `browser.name` matches `browserName()`
3. **Built-in Chrome** → used if `browser.name: chrome`
4. **Built-in Firefox** → used if `browser.name: firefox`
