---
id: report-adapters
title: Report Adapters
sidebar_position: 4
---

# Report Adapters

Selenium Boot generates an HTML report and a JUnit XML file out of the box. For teams that need additional output formats (Allure, Extent, custom dashboards), you can consume the metrics JSON or hook into the reporting pipeline.

---

## Metrics JSON

After every run, Selenium Boot writes a structured JSON file:

```
target/selenium-boot-metrics.json
```

This file contains all test results, durations, retry counts, step data, and aggregate metrics. It is the source of truth for both the HTML report and JUnit XML.

### Sample structure

```json
{
  "total": 25,
  "passed": 23,
  "failed": 1,
  "skipped": 1,
  "passRate": 92.0,
  "flakyTests": 2,
  "recoveredTests": 1,
  "totalDurationMs": 45231,
  "tests": [
    {
      "testId": "LoginTest#validLogin",
      "testClassName": "LoginTest",
      "status": "PASSED",
      "startTime": 1710000000000,
      "endTime": 1710000002341,
      "totalMs": 2341,
      "retryCount": 0,
      "steps": [
        {
          "name": "Open login page",
          "offsetMs": 0,
          "status": "INFO",
          "screenshotBase64": null
        }
      ]
    }
  ]
}
```

---

## Building a custom report from the JSON

Read `selenium-boot-metrics.json` in any language:

```python title="custom_report.py"
import json

with open('target/selenium-boot-metrics.json') as f:
    metrics = json.load(f)

print(f"Pass rate: {metrics['passRate']}%")
for test in metrics['tests']:
    if test['status'] == 'FAILED':
        print(f"  FAILED: {test['testId']} — {test.get('errorMessage', 'no message')}")
```

---

## Allure integration

If your team uses Allure reports, add the Allure TestNG adapter alongside Selenium Boot:

```xml title="pom.xml"
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-testng</artifactId>
    <version>2.25.0</version>
</dependency>
```

Both Selenium Boot listeners and Allure listeners register independently via SPI. They coexist without conflict — you get both report formats from a single test run.

---

## Extent Reports integration

```xml title="pom.xml"
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports</artifactId>
    <version>5.1.1</version>
</dependency>
```

Write a custom `ITestListener` that logs to an ExtentReports instance on `onTestSuccess`, `onTestFailure`, etc. Register it via `testng.xml` or SPI alongside Selenium Boot's built-in listeners.

---

## Sending results to a dashboard

Use a post-build step to `POST` the metrics JSON to your internal dashboard:

```bash title="GitHub Actions"
- name: Send metrics to dashboard
  if: always()
  run: |
    curl -X POST https://dashboard.internal/api/runs \
         -H 'Content-Type: application/json' \
         -d @target/selenium-boot-metrics.json
```
