---
id: junit-xml
title: JUnit XML
sidebar_position: 2
---

# JUnit XML

Selenium Boot generates a JUnit-compatible XML report at `target/surefire-reports/TEST-SeleniumBoot.xml`. This format is understood by virtually every CI system and test reporting tool.

---

## Report location

```
target/
└── surefire-reports/
    └── TEST-SeleniumBoot.xml
```

---

## Format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="SeleniumBoot" tests="12" failures="1" errors="0" skipped="1" time="34.21">

    <testcase classname="LoginTest" name="validLogin" time="2.341"/>

    <testcase classname="CheckoutTest" name="checkout_withInvalidCard" time="1.823">
        <failure message="Expected [Order confirmed] but found [Payment declined]"
                 type="org.openqa.selenium.NoSuchElementException">
            org.openqa.selenium.NoSuchElementException: no such element
                at com.example.tests.CheckoutTest.checkout_withInvalidCard(CheckoutTest.java:42)
                ...
        </failure>
    </testcase>

    <testcase classname="ProfileTest" name="updateAvatar" time="0.0">
        <skipped/>
    </testcase>

</testsuite>
```

---

## Failure messages

The `message` attribute on `<failure>` contains the actual assertion message or exception message from your test — not a generic "Test failed" placeholder.

This means CI tools that display failure summaries (GitHub Actions, Jenkins, Azure Pipelines) show meaningful error descriptions without needing to open the full report.

---

## CI integration

### GitHub Actions — dorny/test-reporter

```yaml
- name: Publish test results
  uses: dorny/test-reporter@v1
  if: always()
  with:
    name: Selenium Boot Results
    path: '**/surefire-reports/TEST-*.xml'
    reporter: java-junit
    fail-on-empty: false
```

### Jenkins

```groovy
post {
    always {
        junit '**/surefire-reports/TEST-*.xml'
    }
}
```

### Azure Pipelines

```yaml
- task: PublishTestResults@2
  condition: always()
  inputs:
    testResultsFormat: JUnit
    testResultsFiles: '**/surefire-reports/TEST-*.xml'
    testRunTitle: 'Selenium Boot'
```

---

## Maven Surefire XML

Maven Surefire also generates its own XML files per class in `target/surefire-reports/`. Both sets of XML files are valid JUnit format and can be published together.

To publish all XML files:

```yaml
path: 'target/surefire-reports/TEST-*.xml'
```
