# Selenium Boot – Getting Started

This guide walks through running your first Selenium Boot test with minimal setup.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Chrome or Firefox installed

No WebDriver binaries required — Selenium Manager handles it automatically.

---

## Step 1: Add the Dependency

```xml
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>0.1.0</version>
</dependency>

<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.9.0</version>
</dependency>
```

Also add the Surefire plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

---

## Step 2: Create `selenium-boot.yml`

Place this file at your **project root** (same level as `pom.xml`):

```yaml
execution:
  mode: local
  baseUrl: https://example.com
  parallel: methods
  threadCount: 4

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

---

## Step 3: Write Your First Test

```java
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SampleTest extends BaseTest {

    @Test
    public void homepageTitleCheck() {
        open();   // navigates to baseUrl
        assertEquals(getDriver().getTitle(), "Example Domain");
    }
}
```

- Extend `BaseTest` — no other setup needed
- Use `open()` to go to `baseUrl`, or `open("/path")` for a sub-path
- Use `getDriver()` to access the WebDriver instance
- Never create or quit WebDriver manually

---

## Step 4: Run Tests

```bash
mvn test
```

---

## Step 5: View the Report

After execution, two files are generated in the `target/` folder:

| File | Description |
|---|---|
| `target/selenium-boot-report.html` | Full HTML report — open in any browser |
| `target/selenium-boot-metrics.json` | Raw metrics in JSON format |

The HTML report includes:
- Suite summary with total / passed / failed / skipped counts
- Per-test status with color coding
- Execution time per test and slowest test highlight
- Failure screenshots linked inline

---

## Next Steps

- Add Page Objects — see the [README](../README.md) for an example
- Enable `@Retryable` on flaky tests
- Switch to remote/Grid execution by changing `execution.mode: remote` and adding `gridUrl`
- Use environment profiles (`selenium-boot-staging.yml`) for multi-environment setups
