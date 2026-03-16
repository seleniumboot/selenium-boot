---
id: quality-gates
title: Quality Gates
sidebar_position: 3
---

# Quality Gates

Fail your CI build when test results fall below an acceptable threshold. This prevents a "green pipeline" from masking widespread test failures.

---

## Maven Surefire failure threshold

By default, Maven fails the build if any test fails. To allow a percentage of failures:

```xml title="pom.xml"
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</plugin>
```

`failIfNoTests: true` ensures the build fails if no tests were discovered — useful as a sanity check.

---

## Fail on zero tests

If your test discovery or suite configuration breaks, Maven will happily report "BUILD SUCCESS" with 0 tests run. Prevent this:

```xml
<configuration>
    <failIfNoTests>true</failIfNoTests>
</configuration>
```

---

## Custom pass-rate gate (shell script)

Parse the JUnit XML output and fail the build if the pass rate drops below a threshold:

```bash title=".github/workflows/test.yml (extra step)"
- name: Quality gate — 90% pass rate required
  run: |
    TOTAL=$(grep -r 'tests=' target/surefire-reports/TEST-*.xml | \
            grep -oP 'tests="\K[0-9]+' | awk '{s+=$1} END {print s}')
    FAILED=$(grep -r 'failures=\|errors=' target/surefire-reports/TEST-*.xml | \
             grep -oP '(failures|errors)="\K[0-9]+' | awk '{s+=$1} END {print s}')
    PASSED=$((TOTAL - FAILED))
    RATE=$(echo "scale=1; $PASSED * 100 / $TOTAL" | bc)
    echo "Pass rate: $RATE% ($PASSED/$TOTAL)"
    if (( $(echo "$RATE < 90" | bc -l) )); then
      echo "FAILED: pass rate $RATE% is below 90% threshold"
      exit 1
    fi
```

---

## dorny/test-reporter gate

When using `dorny/test-reporter` in GitHub Actions, the step fails the workflow if any tests fail:

```yaml
- name: Publish test results
  uses: dorny/test-reporter@v1
  if: always()
  with:
    name: Test Results
    path: '**/surefire-reports/TEST-*.xml'
    reporter: java-junit
    fail-on-error: true     # default true — fails workflow on test failures
    fail-on-empty: false
```

---

## Retry-aware quality gates

Selenium Boot records retry counts in `target/selenium-boot-metrics.json`. A post-build script can check whether too many tests are flaky:

```json title="target/selenium-boot-metrics.json (example)"
{
  "flakyTests": 3,
  "recoveredTests": 2,
  "total": 50,
  "passed": 48
}
```

```bash
- name: Quality gate — max 5% flaky tests
  run: |
    FLAKY=$(cat target/selenium-boot-metrics.json | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('flakyTests', 0))")
    TOTAL=$(cat target/selenium-boot-metrics.json | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('total', 1))")
    RATE=$(echo "scale=1; $FLAKY * 100 / $TOTAL" | bc)
    echo "Flaky rate: $RATE% ($FLAKY/$TOTAL)"
    if (( $(echo "$RATE > 5" | bc -l) )); then
      echo "FAILED: flaky rate $RATE% exceeds 5% threshold"
      exit 1
    fi
```
