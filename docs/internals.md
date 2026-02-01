# Selenium Boot – Internal Design & Execution Model

This document describes the internal design contracts of Selenium Boot.
It defines how execution, lifecycle management, threading, waits, retries, and reporting interact internally.

This document is intended for framework maintainers and advanced contributors.

---

## Java Baseline

- Minimum supported Java version: **Java 17**
- Language features may leverage modern Java constructs
- Backward compatibility below Java 17 is not supported

---

## Core Internal Principles

- One test thread owns exactly one WebDriver instance
- WebDriver lifecycle is framework-managed only
- No shared mutable state across test threads
- Fail fast on misconfiguration or misuse
- Deterministic execution over dynamic behavior

---

## Execution Lifecycle Overview

Selenium Boot integrates with TestNG using listeners and execution hooks.

High-level lifecycle:

1. Framework bootstrap
2. Configuration loading and validation
3. TestNG execution initialization
4. Driver provisioning per thread
5. Test execution with waits and retries
6. Failure evidence capture
7. Report generation
8. Resource cleanup

---

## Configuration Bootstrap

- Configuration is loaded once at startup
- YAML is parsed into immutable configuration objects
- Validation is performed before any test execution
- Invalid configuration fails the build immediately

Configuration objects are read-only during execution.

---

## Driver Lifecycle Model

### Thread Ownership

- Each TestNG thread receives one WebDriver instance
- WebDriver instances are stored using ThreadLocal
- No driver sharing across threads is allowed

### Creation

- Driver is created lazily before first test method execution
- Browser type and execution mode are resolved from configuration
- Driver capabilities are finalized before session creation

### Destruction

- Driver is quit after test execution completes
- Cleanup occurs even in case of test failure or interruption

---

## Parallel Execution Strategy

- Parallel execution is enabled by default
- Thread count is configuration-driven
- Thread safety is enforced at framework boundaries
- Tests must be stateless and independent

Parallelism is controlled centrally to avoid unpredictable behavior.

---

## Wait Strategy

### Explicit Waits

- All waits are explicit and centrally managed
- Timeout values are defined in configuration
- Common wait conditions are standardized

### Implicit Waits

- Implicit waits are explicitly disabled
- Any attempt to enable implicit waits is overridden

This prevents wait compounding and flakiness.

---

## Retry Strategy

### Retry Scope

- Retries apply at the test method level
- Retries are limited to a fixed maximum attempt count
- Retries do not apply to configuration or setup failures

### Failure Classification

- Transient failures may be retried
- Deterministic failures fail immediately
- Retry logic does not suppress final failure status

---

## Failure Handling & Evidence Capture

On test failure:

- Screenshot is captured automatically
- Page source is optionally stored
- Browser logs may be collected (future)

Evidence capture is guaranteed to execute once per failure.

---

## Reporting Pipeline

- Reporting is event-driven via execution hooks
- Reports are generated after execution completion
- Reporting does not affect test execution flow

Report generation failures must not fail the test run.

---

## Logging Strategy

- Framework logs execution lifecycle events
- Test logs remain test-owned
- Logging verbosity is configuration-driven

Logs are structured to support CI environments.

---

## Error Handling Rules

- Framework errors fail fast
- Test assertion failures are isolated
- Infrastructure failures abort execution safely
- Partial execution states are cleaned up deterministically

---

## Extension Points (Controlled)

Framework allows extensions only at defined boundaries:

- Driver providers
- Execution lifecycle hooks
- Reporting adapters
- Configuration overrides

Extensions must not alter core lifecycle guarantees.

---

## Internal Constraints

To preserve correctness:

- No static global state
- No driver access outside framework scope
- No test-managed retries
- No dynamic configuration mutation

Violations are considered framework misuse.

---

## Summary

Selenium Boot internals prioritize correctness, predictability, and maintainability.
The framework defines strict execution contracts to ensure stability across
parallel, long-running, enterprise-scale test suites.
