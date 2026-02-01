# Selenium Boot – MVP Implementation Checklist

This checklist converts design docs into implementation steps.

---

## Phase 1 – Bootstrap

- Create Maven module
- Enforce Java 17 baseline
- Add TestNG dependency

---

## Phase 2 – Configuration

- YAML parser
- Immutable config objects
- Startup validation

---

## Phase 3 – Driver Management

- ThreadLocal WebDriver
- Local browser support
- Safe quit logic

---

## Phase 4 – Execution & Listeners

- SuiteExecutionListener
- TestExecutionListener
- RetryListener

---

## Phase 5 – Wait & Retry

- Central WaitEngine
- Retry rules enforcement

---

## Phase 6 – Reporting

- HTML report generation
- Failure artifacts

---

## Phase 7 – First Green Test

- Sample test
- CI run validation

---

## Summary

Implement in order. Do not skip phases.
