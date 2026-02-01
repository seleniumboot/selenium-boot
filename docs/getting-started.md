# Selenium Boot – Getting Started (MVP)

This guide walks through running your first Selenium Boot test with minimal setup.
The goal is to achieve a successful test run in minutes, not hours.

---

## Prerequisites

Before starting, ensure the following are installed:

- Java 17 or later
- Maven 3.8+
- Git
- A supported browser (Chrome, Firefox, or Edge)

No WebDriver binaries are required.

---

## Create a New Project

Create a standard Maven project or clone the starter repository (when available).

Basic project structure:
```
selenium-boot-project
├── pom.xml
├── selenium-boot.yml
└── src/
    └── test/
        └── java/
```

---

## Maven Configuration

Add Selenium Boot dependency to pom.xml:
```xml
<dependency>
  <groupId>com.seleniumboot</groupId>
  <artifactId>selenium-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

TestNG must be included as the test runner.

---

## Minimal Configuration

Create selenium-boot.yml in the project root:

```yaml
selenium:
  browser:
    name: chrome
  execution:
    mode: local

```

All other settings use default values.

---

## First Test Example

Create a simple TestNG test:

```java
public class SampleTest {

  @Test
  public void openHomePage() {
    driver.get("https://example.com");
    assertEquals(driver.getTitle(), "Example Domain");
  }
}
```

Selenium Boot automatically manages:
- WebDriver lifecycle
- Waits and retries
- Reporting
- Parallel execution

---

## Run Tests

Execute tests using Maven:

```
mvn test
```

Selenium Boot handles setup, execution, and cleanup.

---

## Test Reports

After execution, reports are generated at:

target/reports

Reports include:
- Test results summary
- Failure screenshots
- Execution metadata

---

## Common First-Run Issues

- Browser not installed
- Corporate proxy restrictions
- Insufficient system resources

---

## Next Steps

After the first successful run:

- Adopt the opinionated project structure
- Externalize test data
- Enable parallel execution tuning
- Integrate into CI pipelines

---

## Summary

Selenium Boot is designed to get you from zero to a passing test fast.
Once running, teams can incrementally adopt advanced features without reworking their test suites.
