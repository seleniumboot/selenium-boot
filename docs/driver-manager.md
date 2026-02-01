# Selenium Boot – DriverManager Deep Specification

This document defines the **DriverManager contract** for Selenium Boot.
It specifies ThreadLocal usage, lifecycle timing, failure modes, and non-negotiable rules.

This document is authoritative for all DriverManager implementations.

---

## Purpose of DriverManager

DriverManager is responsible for:

- Creating WebDriver instances
- Binding WebDriver to the executing thread
- Enforcing lifecycle ownership rules
- Safely destroying WebDriver instances
- Preventing driver misuse across tests and threads

DriverManager is the **only component** allowed to manage WebDriver lifecycle.

---

## Core Principles

- One thread → one WebDriver → one lifecycle
- No shared drivers across threads
- No test-owned driver creation or destruction
- Fail fast on incorrect usage
- Deterministic cleanup under all conditions

---

## ThreadLocal Ownership Model

### Storage Strategy

- WebDriver instances are stored using `ThreadLocal<WebDriver>`
- Each TestNG execution thread has exactly one driver reference
- No static or global driver fields are allowed

Example (conceptual):

```
Thread A → Driver A
Thread B → Driver B
Thread C → Driver C
```

Cross-thread access is strictly forbidden.

---

## Driver Creation Rules

### Creation Timing

- Driver creation is **lazy**
- Driver is created immediately before the first test method execution
- Driver creation must not occur at suite or class level

### Configuration Resolution

DriverManager resolves:
- Browser type
- Execution mode (local / remote)
- Browser options
- Capabilities

All values are resolved from immutable configuration.

---

## Driver Access Rules

- Driver access is exposed via framework-controlled APIs only
- Tests access WebDriver through BaseTest or framework context
- Direct calls to DriverManager from tests are forbidden

Attempting to access a driver outside test execution scope results in failure.

---

## Driver Destruction Rules

### Destruction Timing

- Driver is destroyed **after test execution completes**
- Destruction occurs in listener teardown phases
- Cleanup runs even if:
  - Test fails
  - Retry is triggered
  - Execution is interrupted

### Quit Semantics

- `driver.quit()` is always preferred over `close()`
- Multiple quit calls are idempotent-safe
- Exceptions during quit are logged, not rethrown

Driver leaks are treated as framework defects.

---

## Retry Interaction

- Retries reuse the same WebDriver instance
- Driver is not recreated between retries
- Driver is destroyed only after final retry outcome

This ensures:
- Faster retries
- Consistent browser state per test

---

## Parallel Execution Guarantees

- ThreadLocal ensures driver isolation
- No shared mutable driver state
- DriverManager methods must be thread-safe

Parallelism must never compromise correctness.

---

## Failure Modes & Handling

### Driver Creation Failure

Possible causes:
- Browser not installed
- Invalid capabilities
- Remote grid unavailable

Behavior:
- Fail fast
- Abort affected test
- Skip retries
- Mark execution as infrastructure failure

---

### Driver Access Violation

Examples:
- Access outside test method
- Access after driver destruction
- Cross-thread access

Behavior:
- Immediate framework exception
- Test marked as failed
- Execution continues safely

---

### Driver Quit Failure

Behavior:
- Log error with context
- Suppress exception
- Continue cleanup sequence

Execution must not hang due to quit failures.

---

## Forbidden Practices

DriverManager must not:

- Use static WebDriver fields
- Cache drivers across tests
- Expose raw ThreadLocal to tests
- Auto-restart browsers silently
- Mask infrastructure failures

Violations break execution guarantees.

---

## Observability Hooks (Future)

Planned enhancements:
- Driver lifecycle events
- Browser session metadata capture
- Execution timeline correlation

These must not alter core lifecycle behavior.

---

## Summary

DriverManager is the **most critical stability component** in Selenium Boot.
Strict ThreadLocal ownership, deterministic lifecycle control, and explicit
failure handling are mandatory to ensure reliable, parallel, enterprise-scale
test execution.
