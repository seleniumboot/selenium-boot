---
id: cloud-execution
title: Cloud Execution
sidebar_position: 12
---

# Cloud Execution

Selenium Boot supports running your test suite on **BrowserStack** and **Sauce Labs** with zero test-code changes. Switch from local Chrome to a cloud browser farm by changing one line in `selenium-boot.yml`.

---

## How it works

All four execution modes share the same driver lifecycle — tests use `getDriver()`, `open()`, `$()`, and `assertThat()` identically regardless of where the browser runs. The framework picks the right provider based on `execution.mode`.

| Mode | Where it runs |
|---|---|
| `local` | Your machine — Chrome or Firefox via Selenium Manager |
| `remote` | Your own Selenium Grid / Selenoid / Moon |
| `browserstack` | BrowserStack Automate cloud |
| `saucelabs` | Sauce Labs cloud |

---

## BrowserStack

### Prerequisites

1. Sign up at [browserstack.com](https://www.browserstack.com)
2. Go to **Automate** → **Access Key** — copy your username and access key

### Config

```yaml title="selenium-boot.yml"
execution:
  mode: browserstack
  browserstack:
    username:      ${BS_USER}          # set as env var or inline
    accessKey:     ${BS_KEY}
    os:            Windows
    osVersion:     "11"
    browser:       chrome
    browserVersion: latest
```

Set environment variables before running:

```bash
export BS_USER=your_username
export BS_KEY=your_access_key
mvn test
```

### Desktop browsers

```yaml
execution:
  mode: browserstack
  browserstack:
    username:     ${BS_USER}
    accessKey:    ${BS_KEY}
    os:           Windows           # Windows | OS X
    osVersion:    "11"              # 11 | 10 | Sonoma | Ventura | …
    browser:      chrome            # chrome | firefox | edge | safari
    browserVersion: latest          # latest | 120.0 | 119.0 | …
```

### Mobile devices

```yaml
execution:
  mode: browserstack
  browserstack:
    username:     ${BS_USER}
    accessKey:    ${BS_KEY}
    browser:      chrome
    device:       "Samsung Galaxy S23"
    realMobile:   true
```

### Raw capability overrides

Any key under `capabilities` is merged into `bstack:options`:

```yaml
execution:
  mode: browserstack
  browserstack:
    username:   ${BS_USER}
    accessKey:  ${BS_KEY}
    os:         Windows
    osVersion:  "11"
    browser:    chrome
    capabilities:
      debug: true
      networkLogs: true
      consoleLogs: verbose
      video: true
```

### Session link in HTML report

After each test, the BrowserStack session URL is captured automatically. A **☁ View Session** link appears in the test detail panel — click it to open the BrowserStack dashboard with video, network logs, and console output for that specific test run.

---

## Sauce Labs

### Prerequisites

1. Sign up at [saucelabs.com](https://saucelabs.com)
2. Go to **Account** → **User Settings** → copy your username and access key

### Config

```yaml title="selenium-boot.yml"
execution:
  mode: saucelabs
  saucelabs:
    username:      ${SAUCE_USER}
    accessKey:     ${SAUCE_KEY}
    region:        us-west-1          # us-west-1 | eu-central | apac-southeast
    platformName:  "Windows 11"
    browser:       chrome
    browserVersion: latest
```

### Regions

| Value | Data centre |
|---|---|
| `us-west-1` | US West (default) |
| `eu-central` | EU (Frankfurt) |
| `apac-southeast` | APAC (Southeast Asia) |

### Raw capability overrides

Keys under `capabilities` are merged into `sauce:options`:

```yaml
execution:
  mode: saucelabs
  saucelabs:
    username:   ${SAUCE_USER}
    accessKey:  ${SAUCE_KEY}
    region:     eu-central
    platformName: "Windows 11"
    browser:    chrome
    browserVersion: latest
    capabilities:
      tags: ["regression", "nightly"]
      build: "v2.1.0"
      recordVideo: true
```

### Session link in HTML report

Same as BrowserStack — a **☁ View Session** link appears in each test's detail panel, linking to the Sauce Labs test dashboard with video and logs.

---

## Parallel execution on cloud

Parallel config works the same as local — set it in `selenium-boot.yml` and the cloud provider scales accordingly:

```yaml
execution:
  mode: browserstack
  parallel:    methods
  threadCount: 4        # 4 concurrent BrowserStack sessions
  browserstack:
    username:  ${BS_USER}
    accessKey: ${BS_KEY}
    os:        Windows
    osVersion: "11"
    browser:   chrome
    browserVersion: latest
```

:::info Session limits
Ensure your BrowserStack / Sauce Labs plan allows the number of concurrent sessions you configure in `threadCount`. The framework's semaphore-based session guard still applies.
:::

---

## Switching between environments

Use Maven profiles or environment variables to switch execution targets without changing any YAML file:

```bash
# Local
mvn test

# BrowserStack
BS_USER=user BS_KEY=key mvn test -Dselenium.boot.config=config/browserstack.yml

# Sauce Labs
SAUCE_USER=user SAUCE_KEY=key mvn test -Dselenium.boot.config=config/saucelabs.yml
```

Keep separate YAML files per environment — each imports shared settings and only overrides `execution.mode` and cloud credentials.

---

## Full config reference

```yaml title="selenium-boot.yml"
execution:
  mode: browserstack   # local | remote | browserstack | saucelabs

  # ── BrowserStack ───────────────────────────────────────────────
  browserstack:
    username:      ${BS_USER}
    accessKey:     ${BS_KEY}
    os:            Windows          # Windows | OS X
    osVersion:     "11"
    browser:       chrome           # chrome | firefox | edge | safari
    browserVersion: latest
    device:                         # optional — mobile device name
    realMobile:    true
    capabilities:                   # raw bstack:options overrides
      debug: false
      networkLogs: false

  # ── Sauce Labs ─────────────────────────────────────────────────
  saucelabs:
    username:      ${SAUCE_USER}
    accessKey:     ${SAUCE_KEY}
    region:        us-west-1        # us-west-1 | eu-central | apac-southeast
    platformName:  "Windows 11"
    browser:       chrome
    browserVersion: latest
    capabilities:                   # raw sauce:options overrides
      recordVideo: true
```
