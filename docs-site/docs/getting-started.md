---
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

---

## Step 1 — Add the dependency

<Tabs>
<TabItem value="maven" label="Maven (pom.xml)">

```xml title="pom.xml"
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>2.6.0</version>
</dependency>
```

</TabItem>
<TabItem value="gradle-groovy" label="Gradle Groovy (build.gradle)">

```groovy title="build.gradle"
dependencies {
    testImplementation 'io.github.seleniumboot:selenium-boot:3.0.0'
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
    testImplementation("io.github.seleniumboot:selenium-boot:3.0.0")
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

## Step 2 — Create the configuration file

Create `selenium-boot.yml` in your project root (next to `pom.xml` or `build.gradle`):

```yaml title="selenium-boot.yml"
browser:
  name: chrome
  headless: false

execution:
  baseUrl: https://your-app.com

retry:
  enabled: true
  maxAttempts: 2

timeouts:
  explicit: 10
  pageLoad: 30
```

---

## Step 3 — Write your first test

```java title="src/test/java/com/example/LoginTest.java"
import com.seleniumboot.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(description = "Valid user can log in")
    public void loginTest() {
        open();  // navigates to baseUrl
        // your test steps here
        Assert.assertTrue(getDriver().getTitle().contains("Dashboard"));
    }
}
```

---

## Step 4 — Create a TestNG suite

```xml title="testng.xml"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="selenium-boot-suite" verbose="1">
    <test name="MyTests">
        <classes>
            <class name="com.example.LoginTest"/>
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

1. Framework loads `selenium-boot.yml`
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
├── selenium-boot.yml
├── testng.xml
└── src/test/java/com/example/
    ├── pages/LoginPage.java
    └── tests/LoginTest.java
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```
your-project/
├── build.gradle (or build.gradle.kts)
├── selenium-boot.yml
├── testng.xml
└── src/test/java/com/example/
    ├── pages/LoginPage.java
    └── tests/LoginTest.java
```

</TabItem>
</Tabs>

---

## Working example project

A complete working project is available at:
**https://github.com/seleniumboot/selenium-boot-test**

Clone it, run `mvn test` (or `./gradlew test`), and you'll have a full working suite with page objects, step logging, and retry configured.

---

## Next steps

- [Configuration Reference](/docs/configuration) — all available config options
- [BasePage](/docs/guides/base-page) — write clean page objects
- [Step Logging](/docs/guides/step-logging) — add named steps to your tests
