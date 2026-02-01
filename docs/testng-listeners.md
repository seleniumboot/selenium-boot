# Selenium Boot – TestNG Listener Wiring Specification

This document defines how Selenium Boot integrates with TestNG listeners.
It formalizes listener responsibilities, execution order, and lifecycle boundaries.

This specification is **contractual** and must be followed by all framework implementations.

---

## Design Goals

The listener system is designed to:

- Centralize execution control
- Prevent test-managed lifecycle logic
- Guarantee deterministic driver handling
- Ensure reliable reporting and cleanup
- Support parallel execution safely

---

## Listener Registration Strategy

Selenium Boot registers TestNG listeners using **ServiceLoader-based auto-registration** or explicit `@Listeners` binding via the base test class.

Users must not manually register framework listeners.

---

## Listener Set (MVP)

The MVP uses three core listeners:

1. SuiteExecutionListener  
2. TestExecutionListener  
3. RetryListener  

Each listener has a strictly defined responsibility.

---

## Listener Execution Order

High-level execution sequence:

1. SuiteExecutionListener.onStart  
2. TestExecutionListener.onStart (per test)  
3. RetryListener (on failure only)  
4. TestExecutionListener.onFinish (per test)  
5. SuiteExecutionListener.onFinish  

This order must not be altered.

---

## SuiteExecutionListener

Implements:
- ISuiteListener

Responsibilities:
- Framework bootstrap
- Configuration loading and validation
- Global execution context initialization
- Parallel execution model setup

Constraints:
- Must not create WebDriver instances
- Must not access test classes
- Executes once per suite

Failure Behavior:
- Any failure aborts the entire execution

---

## TestExecutionListener

Implements:
- ITestListener

Responsibilities:
- Driver provisioning before test method execution
- Thread-local driver binding
- Failure evidence capture
- Reporting lifecycle hooks
- Driver cleanup after test execution

Constraints:
- Must not modify TestNG execution flow
- Must not suppress test failures
- Must not retry tests

Driver Lifecycle:
- Driver is created lazily before first test method
- Driver is destroyed after test execution completes

---

## RetryListener

Implements:
- IRetryAnalyzer

Responsibilities:
- Decide whether a failed test should be retried
- Enforce retry limits
- Track retry attempts

Constraints:
- Applies only to test method failures
- Must not retry configuration failures
- Must not alter final test result semantics

Retry decisions are deterministic and configuration-driven.

---

## Failure Handling Flow

On test failure:

1. TestExecutionListener captures screenshot
2. RetryListener evaluates retry eligibility
3. If retry allowed:
   - Test method is re-executed
4. If retry exhausted:
   - Failure is reported
5. Execution continues safely

Evidence capture occurs once per failure occurrence.

---

## Parallel Execution Considerations

- Each TestNG thread owns one WebDriver instance
- Listener code must be thread-safe
- No shared mutable state across listeners
- ThreadLocal is the only allowed driver storage mechanism

---

## Reporting Integration

- Reporting hooks are triggered by TestExecutionListener
- Report generation occurs after suite completion
- Reporting failures must not affect test execution

---

## Listener Error Handling Rules

- Listener initialization failures abort execution
- Runtime listener errors are treated as framework errors
- Cleanup logic must always execute, even on failure

---

## Explicitly Forbidden Actions

Listeners must not:

- Create static WebDriver fields
- Modify TestNG execution order dynamically
- Access test data directly
- Read configuration files directly
- Swallow or mask test failures

Violations are considered framework defects.

---

## Summary

The TestNG listener system is the **control spine** of Selenium Boot.
Strict listener boundaries ensure predictable execution, stable parallelism,
and enterprise-grade reliability.
