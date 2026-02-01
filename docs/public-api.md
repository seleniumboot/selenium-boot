# Selenium Boot – Public API Contract

This document defines the **public API surface** of Selenium Boot.
Anything not explicitly listed here is considered **internal** and may change without notice.

---

## Purpose

- Protect users from internal changes
- Prevent accidental framework misuse
- Freeze long-term supported APIs

---

## Supported Public APIs

### BaseTest
Users must extend `BaseTest` for all test classes.

Allowed:
- Access to WebDriver
- Access to framework utilities

Forbidden:
- Driver creation or destruction
- Lifecycle overrides

---

### BasePage
Used for Page Object Models.

Allowed:
- Element interactions
- Wait utilities

Forbidden:
- Assertions
- Driver lifecycle logic

---

### Configuration (selenium-boot.yml)

- All documented configuration keys are public
- Undocumented keys are unsupported

---

## Explicitly Non-Public APIs

- DriverManager
- ExecutionEngine
- Internal listeners
- Context objects

Direct usage is forbidden.

---

## Compatibility Guarantee

- Public APIs are stable post v1.0
- Breaking changes require major version bump

---

## Summary

If it’s not listed here, don’t depend on it.
