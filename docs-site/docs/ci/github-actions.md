---
id: github-actions
title: GitHub Actions
sidebar_position: 1
---

# GitHub Actions

Run your Selenium Boot tests on every push and pull request. The workflow below installs Chrome, runs the suite, and uploads the HTML report as a downloadable artifact.

---

## Basic workflow

```yaml title=".github/workflows/test.yml"
name: Selenium Tests

on:
  push:
    branches: [main, master]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Install Chrome
        uses: browser-actions/setup-chrome@v1

      - name: Run tests
        run: mvn test -B

      - name: Upload HTML report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: selenium-boot-report
          path: target/selenium-boot-report.html
```

---

## Headless Chrome

Chrome on CI runners must run headless. Configure this in `selenium-boot.yml`:

```yaml title="selenium-boot.yml"
browser:
  type: chrome
  headless: true
```

Or set it only in CI using an environment variable override (if supported by your config loading):

```yaml
      - name: Run tests
        run: mvn test -B
        env:
          SELENIUM_HEADLESS: true
```

---

## Publish JUnit XML test results

```yaml
      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Test Results
          path: '**/surefire-reports/TEST-*.xml'
          reporter: java-junit
          fail-on-empty: false
```

This renders pass/fail counts directly in the GitHub Actions summary and PR checks.

---

## Matrix — multiple browsers

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        browser: [chrome, firefox]

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Install Chrome
        if: matrix.browser == 'chrome'
        uses: browser-actions/setup-chrome@v1

      - name: Install Firefox
        if: matrix.browser == 'firefox'
        uses: browser-actions/setup-firefox@v1

      - name: Run tests
        run: mvn test -B -Dbrowser.type=${{ matrix.browser }}

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: report-${{ matrix.browser }}
          path: target/selenium-boot-report.html
```

---

## Caching Maven dependencies

The `cache: maven` option in `setup-java` caches `~/.m2/repository` automatically. This significantly reduces build time on subsequent runs.

---

## Full example with parallel tests

```yaml
      - name: Run tests
        run: mvn test -B -Dparallel=methods -DthreadCount=4
```

Or define parallel settings in `selenium-boot.yml` and commit it — the CI runner picks them up automatically.
