---
id: gradle
title: Gradle Build Support
sidebar_position: 3
---

# Gradle Build Support

Selenium Boot works with Gradle out of the box — the JAR on Maven Central is build-tool-agnostic. This page covers the recommended setup for both Groovy DSL (`build.gradle`) and Kotlin DSL (`build.gradle.kts`).

---

## Step 1 — Add the dependency

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs>
<TabItem value="groovy" label="Groovy DSL (build.gradle)">

```groovy title="build.gradle"
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'io.github.seleniumboot:selenium-boot:2.6.0'
}
```

</TabItem>
<TabItem value="kotlin" label="Kotlin DSL (build.gradle.kts)">

```kotlin title="build.gradle.kts"
plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.seleniumboot:selenium-boot:2.6.0")
}
```

</TabItem>
</Tabs>

---

## Step 2 — Configure test execution

### TestNG (default runner)

<Tabs>
<TabItem value="groovy" label="Groovy DSL">

```groovy title="build.gradle"
test {
    useTestNG {
        // Optional: point to a testng.xml suite file
        // suites 'src/test/resources/testng.xml'
    }

    // Forward system properties so -Denv=staging works from the CLI
    systemProperties System.properties

    // Display test output in the console
    testLogging {
        events 'passed', 'skipped', 'failed'
        showStandardStreams = false
    }
}
```

</TabItem>
<TabItem value="kotlin" label="Kotlin DSL">

```kotlin title="build.gradle.kts"
tasks.test {
    useTestNG {
        // Optional: point to a testng.xml suite file
        // suites("src/test/resources/testng.xml")
    }

    // Forward system properties so -Denv=staging works from the CLI
    systemProperties(System.getProperties().mapKeys { it.key.toString() })

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
```

</TabItem>
</Tabs>

### JUnit 5 bridge

If you're using `BaseJUnit5Test` or `@EnableSeleniumBoot`:

<Tabs>
<TabItem value="groovy" label="Groovy DSL">

```groovy title="build.gradle"
dependencies {
    testImplementation 'io.github.seleniumboot:selenium-boot:2.6.0'
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.2'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.2'
}

test {
    useJUnitPlatform()
    systemProperties System.properties
}
```

</TabItem>
<TabItem value="kotlin" label="Kotlin DSL">

```kotlin title="build.gradle.kts"
dependencies {
    testImplementation("io.github.seleniumboot:selenium-boot:2.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
}
```

</TabItem>
</Tabs>

---

## Step 3 — Configuration file

Create `selenium-boot.yml` at the **project root** (same level as `build.gradle`):

```yaml title="selenium-boot.yml"
execution:
  mode: local
  baseUrl: https://example.com

browser:
  name: chrome
  headless: true

retry:
  enabled: true
  maxAttempts: 2
```

---

## Running tests

```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.LoginTest"

# Run a single test method
./gradlew test --tests "com.example.LoginTest.validLogin"

# Run with an environment profile
./gradlew test -Denv=staging

# Pass multiple JVM args
./gradlew test -Dbrowser.name=firefox -Dbrowser.headless=true
```

---

## Test report locations

| Report type | Gradle path |
|---|---|
| HTML report (Selenium Boot) | `build/selenium-boot-report/index.html` |
| JUnit XML (Selenium Boot) | `build/test-results/test/TEST-SeleniumBoot.xml` |
| Gradle's own HTML report | `build/reports/tests/test/index.html` |
| Allure results (if enabled) | `build/allure-results/` |

:::info JUnit XML auto-detection
Selenium Boot automatically detects Gradle by checking whether a `build/` directory exists and `target/` does not, then writes XML to `build/test-results/test/`. Override with `-Dseleniumboot.reports.dir=path/to/dir` if needed.
:::

---

## Parallel execution

For parallel runs with Gradle, configure the `test` task alongside `selenium-boot.yml`:

<Tabs>
<TabItem value="groovy" label="Groovy DSL">

```groovy title="build.gradle"
test {
    useTestNG()
    maxParallelForks = 4          // Gradle worker processes
    systemProperties System.properties
}
```

</TabItem>
<TabItem value="kotlin" label="Kotlin DSL">

```kotlin title="build.gradle.kts"
tasks.test {
    useTestNG()
    maxParallelForks = 4
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
}
```

</TabItem>
</Tabs>

```yaml title="selenium-boot.yml"
execution:
  parallel: methods
  threadCount: 4
```

---

## Optional dependencies

These are `compileOnly` / optional in the Selenium Boot JAR — add them only if you use the corresponding feature:

| Feature | Dependency |
|---|---|
| Excel `@TestData` | `testImplementation 'org.apache.poi:poi-ooxml:5.2.5'` |
| Email verification (IMAP) | `testImplementation 'com.sun.mail:jakarta.mail:2.0.1'` |
| Cucumber | `testImplementation 'io.cucumber:cucumber-java:7.15.0'` + `testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.15.0'` |

---

## Full example project

A minimal working Gradle project (Groovy DSL, TestNG):

```
my-tests/
├── build.gradle
├── selenium-boot.yml
└── src/
    └── test/
        └── java/
            └── com/example/
                └── LoginTest.java
```

```groovy title="build.gradle"
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'io.github.seleniumboot:selenium-boot:2.6.0'
}

test {
    useTestNG()
    systemProperties System.properties
    testLogging { events 'passed', 'skipped', 'failed' }
}
```

```java title="src/test/java/com/example/LoginTest.java"
import com.seleniumboot.test.BaseTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    public void validLogin() {
        open("/login");
        $("input#username").type("admin");
        $("input#password").type("secret");
        $("button[type='submit']").click();
        assertThat(By.id("dashboard")).isVisible();
    }
}
```

```bash
./gradlew test
```
