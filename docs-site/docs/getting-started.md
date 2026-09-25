---
description: "Get your first Selenium test running in under 5 minutes: add one dependency, extend BaseTest, and run, with no WebDriver setup or boilerplate."
id: getting-started
title: Getting Started
sidebar_position: 2
---

# Getting Started

Get your first Selenium Boot test running in under 5 minutes.

---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Prerequisites

- Java 17+
- Maven 3.8+ **or** Gradle 7+
- Chrome or Firefox installed

:::info
No WebDriver binaries required — Selenium Manager handles browser driver downloads automatically.
:::

:::tip Skip the setup
Prefer to start from a working project? Click **Use this template** on
[selenium-boot-starter](https://github.com/seleniumboot/selenium-boot-starter) — Maven, TestNG,
config, a page object and passing tests, ready for `mvn test`.
:::

---

## Step 1 — Add the dependency

<Tabs>
<TabItem value="maven" label="Maven (pom.xml)">

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>3.5.0</version>
</dependency>
```

</TabItem>
<TabItem value="gradle-groovy" label="Gradle Groovy (build.gradle)">

```groovy title="build.gradle"
dependencies {
    testImplementation 'io.github.seleniumboot:selenium-boot:3.5.0'
}

test {
    useTestNG()
    systemProperties System.properties
}
```

</TabItem>
<TabItem value="gradle-kotlin" label="Gradle Kotlin (build.gradle.kts)">

```kotlin title="build.gradle.kts"
dependencies {
    testImplementation("io.github.seleniumboot:selenium-boot:3.5.0")
}

tasks.test {
    useTestNG()
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
}
```

</TabItem>
</Tabs>

:::tip Using Gradle?
See the full [Gradle Setup Guide](/docs/gradle) for parallel config, JUnit 5, optional deps, and report locations.
:::

---

## Step 2 — Configuration file (optional)

`selenium-boot.yml` is optional — with no file at all the framework runs on built-in
defaults (Chrome, local execution, 10 s / 30 s timeouts) and prints one notice line
saying so. Create it in your project root (next to `pom.xml` or `build.gradle`) when
you want to change anything, such as the `baseUrl` below.

:::note Step 3 needs this file
There is no default `baseUrl`, so `open()` in Step 3 only works once
`execution.baseUrl` is set. Create the file below to follow this walkthrough. Without it,
navigate with `getDriver().get("https://example.com")` instead.
:::

This example uses `https://example.com` — a stable real site reserved for
documentation — so you can copy the files as-is and `mvn test` goes green.
Swap in your own URL once it passes.

```yaml title="selenium-boot.yml"
execution:
  mode: local
  baseUrl: https://example.com

browser:
  name: chrome
  headless: false

retry:
  enabled: true
  maxAttempts: 2

timeouts:
  explicit: 10
  pageLoad: 30
```

Every field here is optional — anything you leave out falls back to a default, same as an
absent file. See the [configuration reference](configuration.md) for the full list.

---

## Step 3 — Write your first test

Copy this as-is — with the `selenium-boot.yml` from Step 2 in place, it passes against its `baseUrl` with a real
Chrome, no changes needed:

```java title="src/test/java/SmokeTest.java"
import com.seleniumboot.locator.Role;
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

public class SmokeTest extends BaseTest {

    @Test
    public void opensThePage() {
        open();  // navigates to baseUrl
        assertThat(getByRole(Role.HEADING, "Example Domain")).isVisible();
    }
}
```

`getByRole` finds elements the way a screen reader does, and
`assertThat(...).isVisible()` retries until the timeout instead of failing on
the first miss — so no `WebDriverWait`, no CSS selectors, no `Thread.sleep()`.

:::tip Testing your own app?
Once this passes, point `execution.baseUrl` at your app and replace the
assertion with a locator for your page — e.g.
`assertThat(getByRole(Role.BUTTON, "Sign in")).isVisible()`.
:::

---

## Step 4 — Create a TestNG suite

```xml title="testng.xml"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="selenium-boot-suite" verbose="1">
    <test name="MyTests">
        <classes>
            <class name="SmokeTest"/>
        </classes>
    </test>
</suite>
```

---

## Step 5 — Run

<Tabs>
<TabItem value="maven" label="Maven">

```bash
mvn test
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```bash
./gradlew test
```

</TabItem>
</Tabs>

---

## What happens

1. Framework loads `selenium-boot.yml` (or built-in defaults if it's absent)
2. Chrome launches automatically
3. Your test runs
4. Screenshot captured on any failure
5. Browser closes
6. HTML report generated at `target/selenium-boot-report.html` (Maven) or `build/selenium-boot-report/` (Gradle)
7. Metrics JSON at `target/selenium-boot-metrics.json`

---

## Project structure

<Tabs>
<TabItem value="maven" label="Maven">

```
your-project/
├── pom.xml
├── selenium-boot.yml   # optional — omit to run on built-in defaults
├── testng.xml
└── src/test/java/
    └── SmokeTest.java
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```
your-project/
├── build.gradle (or build.gradle.kts)
├── selenium-boot.yml   # optional — omit to run on built-in defaults
├── testng.xml
└── src/test/java/
    └── SmokeTest.java
```

</TabItem>
</Tabs>

---

## Working example project

The [starter template](https://github.com/seleniumboot/selenium-boot-starter) is the smallest
project that runs. For a larger one, a complete working project is available at:
**https://github.com/seleniumboot/selenium-boot-test**

Clone it, run `mvn test` (or `./gradlew test`), and you'll have a full working suite with page objects, step logging, and retry configured.

---

## Next steps

- [Configuration Reference](/docs/configuration) — all available config options
- [BasePage](/docs/guides/base-page) — write clean page objects
- [Step Logging](/docs/guides/step-logging) — add named steps to your tests
