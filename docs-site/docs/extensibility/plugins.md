---
id: plugins
title: Plugins (TestNG Listeners)
sidebar_position: 3
---

# Plugins (TestNG Listeners)

Selenium Boot's own listeners (`TestExecutionListener`, `SuiteExecutionListener`, `RetryAnnotationTransformer`) are auto-registered via Java SPI. You can register your own TestNG listeners in exactly the same way.

---

## Register a listener via `testng.xml`

The simplest approach — add your listener to the suite XML:

```xml title="testng.xml"
<suite name="MyTests">
    <listeners>
        <listener class-name="com.example.MyCustomListener"/>
    </listeners>
    <test name="All Tests">
        <classes>
            <class name="com.example.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

---

## Register a listener via Java SPI

To auto-register a listener whenever your project is on the classpath (useful for shared libraries):

1. Create your listener class:

```java
package com.example;

import org.testng.ITestListener;
import org.testng.ITestResult;

public class SlackNotificationListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        // post a Slack message when a test fails
        SlackClient.send("#alerts", "FAILED: " + result.getName());
    }
}
```

2. Create the SPI registration file:

```
src/main/resources/META-INF/services/org.testng.ITestNGListener
```

Contents:

```
com.example.SlackNotificationListener
```

TestNG discovers and instantiates this listener automatically at runtime.

---

## Access Selenium Boot context in a listener

Use `SeleniumBootContext` to read the current config or test ID:

```java
import com.seleniumboot.context.SeleniumBootContext;

public class MyListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        String testId = SeleniumBootContext.getCurrentTestId();
        String baseUrl = SeleniumBootContext.getConfig().getBrowser().getBaseUrl();
        System.out.println("Starting " + testId + " against " + baseUrl);
    }
}
```

---

## Access the WebDriver in a listener

```java
import com.seleniumboot.driver.DriverManager;

public class MyListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            String pageSource = driver.getPageSource();
            // save page source for debugging
        }
    }
}
```

---

## ISuiteListener

For suite-level events:

```java
public class SuiteTimingListener implements ISuiteListener {

    private long suiteStart;

    @Override
    public void onStart(ISuite suite) {
        suiteStart = System.currentTimeMillis();
    }

    @Override
    public void onFinish(ISuite suite) {
        long elapsed = System.currentTimeMillis() - suiteStart;
        System.out.println("Suite completed in " + elapsed + "ms");
    }
}
```
