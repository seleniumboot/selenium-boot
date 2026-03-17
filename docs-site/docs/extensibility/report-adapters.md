---
id: report-adapters
title: Report Adapters
sidebar_position: 4
---

# Report Adapters

`ReportAdapter` lets you generate any output format from the metrics JSON — Slack messages, Allure input, email summaries, custom dashboards. The built-in HTML adapter always runs; your adapters are appended after it.

---

## Create a report adapter

```java
import com.seleniumboot.reporting.ReportAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

public class SlackReportAdapter implements ReportAdapter {

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public void generate(File metricsJson) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(metricsJson);

        int total    = root.path("total").asInt();
        int passed   = root.path("passed").asInt();
        int failed   = root.path("failed").asInt();
        double rate  = root.path("passRate").asDouble();

        String message = String.format(
            "Test run complete — %d/%d passed (%.1f%%)%s",
            passed, total, rate,
            failed > 0 ? " :red_circle: " + failed + " failures" : " :white_check_mark:"
        );

        SlackClient.post("#test-results", message);
    }
}
```

---

## Register via Java SPI (auto-discovery)

```
src/main/resources/META-INF/services/com.seleniumboot.reporting.ReportAdapter
```

Contents:

```
com.example.reporting.SlackReportAdapter
```

---

## Register programmatically

```java
import com.seleniumboot.reporting.ReportAdapterRegistry;

ReportAdapterRegistry.register(new SlackReportAdapter());
```

---

## Metrics JSON structure

The `metricsJson` file (`target/selenium-boot-metrics.json`) passed to `generate()` contains:

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
      "errorMessage": null,
      "stackTrace": null,
      "steps": [
        { "name": "Open login page", "offsetMs": 0, "status": "INFO", "screenshotBase64": null }
      ]
    }
  ]
}
```

---

## Adapter execution order

1. Built-in `HtmlReportAdapter` (always first)
2. SPI-discovered adapters (in discovery order)
3. Programmatically registered adapters

Each adapter runs independently — a failure in one is logged but does not prevent others from running.

---

## Allure integration

Run Allure alongside Selenium Boot by adding the Allure TestNG dependency. Both register listeners independently via SPI:

```xml title="pom.xml"
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-testng</artifactId>
    <version>2.25.0</version>
</dependency>
```

No `ReportAdapter` needed — Allure hooks directly into TestNG. You get both the Selenium Boot HTML report and a full Allure report from a single run.
