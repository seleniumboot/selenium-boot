# Selenium Boot – Retry & Wait Interaction Specification

This document defines how **retry logic** and **wait strategies** interact inside Selenium Boot.
Its primary goal is to reduce flakiness **without masking real failures**.

This specification is authoritative and must be followed by all implementations.

---

## Design Goals

The retry + wait system is designed to:

- Eliminate timing-related flakiness
- Prevent excessive or compounding waits
- Avoid hiding deterministic failures
- Keep test execution predictable and debuggable
- Fail fast when the application is genuinely broken

---

## Core Principles

- Waits handle *timing uncertainty*
- Retries handle *execution instability*
- Waits and retries must never amplify each other
- Retries are limited, explicit, and visible
- Final failure must always surface

---

## Wait Strategy Contract

### Explicit Waits Only

- Selenium Boot uses **explicit waits exclusively**
- Implicit waits are enforced as `0` at all times
- Any attempt to enable implicit waits is overridden

Rationale:
Implicit waits + retries = exponential flakiness.

---

### Centralized Wait Engine

- All waits are routed through `WaitEngine`
- Timeout values come only from configuration
- No hard-coded sleeps are allowed

Allowed:
- Visibility waits
- Clickability waits
- Presence waits
- URL / title conditions

Forbidden:
- `Thread.sleep`
- Custom polling loops in tests

---

## Retry Strategy Contract

### Retry Scope

- Retries apply **only at the test method level**
- Retries do not apply to:
  - Configuration failures
  - Driver creation failures
  - Assertion logic errors caused by invalid test data

---

### Retry Limits

- Retry count is configuration-driven
- Default retry count is intentionally low (e.g., 1–2)
- Infinite or unbounded retries are forbidden

Retries must be visible in reports.

---

## Retry + Wait Interaction Rules

### Rule 1: No Wait Escalation on Retry

- Retry does **not** increase wait timeouts
- Wait configuration remains constant across retries

Why:
Escalating waits hides performance regressions and real defects.

---

### Rule 2: Same Driver, Same State

- Retries reuse the same WebDriver instance
- Browser is not restarted between retries
- Application state remains visible for debugging

Why:
Restarting browsers hides state-related bugs.

---

### Rule 3: Waits Execute Before Retry Decision

Execution order:
1. Explicit waits are applied
2. Action is attempted
3. Assertion is evaluated
4. Failure occurs
5. Retry decision is made

Retries must not bypass waits.

---

### Rule 4: Retry Stops on Deterministic Failures

Examples of deterministic failures:
- Element not found after explicit wait timeout
- Assertion mismatch with static expected value
- HTTP 500 page consistently returned

Behavior:
- Retry is skipped
- Test fails immediately

---

### Rule 5: Retry Allowed for Transient Failures

Examples:
- Stale element reference
- Temporary rendering delay
- Intermittent click interception

Retry eligibility is evaluated using failure classification.

---

## Failure Classification Model (MVP)

Failures are classified as:

### Transient Failures
Eligible for retry:
- StaleElementReferenceException
- ElementClickInterceptedException
- Timeout due to slow rendering

### Deterministic Failures
Not eligible for retry:
- Assertion failures on business logic
- Invalid locators
- Missing elements after full explicit wait
- Configuration errors

Classification rules must be conservative.

---

## Reporting Requirements

- Each retry attempt is recorded
- Wait timeouts are logged
- Final failure includes:
  - Number of retries attempted
  - Last failure reason
  - Screenshot from final attempt

Retries must never silently pass.

---

## Anti-Patterns Explicitly Forbidden

- Increasing waits instead of fixing tests
- Using retries to hide broken assertions
- Sleeping instead of waiting
- Retrying setup or teardown logic
- Retrying indefinitely

These practices create false stability.

---

## Debuggability Guarantees

When a test fails, engineers must be able to answer:

- Did the wait expire?
- Was a retry attempted?
- Why was the retry allowed or skipped?
- What was the final browser state?

Retry + wait logic must make failures **easier**, not harder, to debug.

---

## Summary

Selenium Boot treats waits and retries as **surgical tools**, not blunt instruments.
Strict separation, conservative retry rules, and fixed wait boundaries ensure
reliable execution without masking real application defects.
