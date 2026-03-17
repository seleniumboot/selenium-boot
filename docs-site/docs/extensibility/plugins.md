---
id: plugins
title: Plugins
sidebar_position: 1
---

# Plugins

`SeleniumBootPlugin` is the main extension point for adding behaviour that runs alongside the framework. Plugins are discovered automatically via Java SPI — no registration code needed in your tests.

---

## Create a plugin

```java
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.extension.SeleniumBootPlugin;

public class SlackNotificationPlugin implements SeleniumBootPlugin {

    @Override
    public String getName() {
        return "slack-notification";
    }

    @Override
    public String minFrameworkVersion() {
        return "0.7.0";   // framework checks this before loading
    }

    @Override
    public void onLoad(SeleniumBootConfig config) {
        // called once after config is loaded — read settings, open connections
        String baseUrl = config.getBrowser().getBaseUrl();
        System.out.println("SlackPlugin initialised for " + baseUrl);
    }

    @Override
    public void onUnload() {
        // called once after all reports are generated — flush, close, clean up
    }
}
```

---

## Register via Java SPI (auto-discovery)

Create the SPI registration file in your project:

```
src/main/resources/META-INF/services/com.seleniumboot.extension.SeleniumBootPlugin
```

Contents — one fully-qualified class name per line:

```
com.example.plugins.SlackNotificationPlugin
```

Selenium Boot discovers and loads this plugin automatically when your JAR is on the classpath. No listener registration, no config entries.

---

## Register programmatically

For plugins that need to be registered before framework boot:

```java
import com.seleniumboot.extension.PluginRegistry;
import com.seleniumboot.context.SeleniumBootContext;

PluginRegistry.register(new SlackNotificationPlugin(), SeleniumBootContext.getConfig());
```

---

## Version compatibility

Declare the minimum framework version your plugin requires:

```java
@Override
public String minFrameworkVersion() {
    return "0.7.0";
}
```

If the running framework is older, the plugin is **skipped with a warning** — it will not fail the build. You can also assert from inside `onLoad`:

```java
import com.seleniumboot.extension.FrameworkVersion;

@Override
public void onLoad(SeleniumBootConfig config) {
    FrameworkVersion.requireAtLeast("0.7.0");  // throws IncompatiblePluginException if too old
}
```

Check the current version at runtime:

```java
String version = FrameworkVersion.get();  // e.g. "0.7.0"
```

---

## Plugin lifecycle

```
Suite starts
  → PluginRegistry.loadAll()         // SPI discovery + onLoad() called
  → [all tests run]
  → Reports generated
  → PluginRegistry.unloadAll()       // onUnload() called on every plugin
Suite ends
```

`onLoad` failures are logged but do not abort the suite. `onUnload` failures are also isolated.

---

## What plugins are good for

| Use case | Approach |
|---|---|
| Suite-level setup/teardown | `onLoad` / `onUnload` |
| Reading framework config | `onLoad(SeleniumBootConfig config)` |
| Initialising external clients | `onLoad` |
| Flushing metrics / closing connections | `onUnload` |
| Per-test events | Use [`ExecutionHook`](/docs/extensibility/hooks) instead |
| Custom report generation | Use [`ReportAdapter`](/docs/extensibility/report-adapters) instead |
