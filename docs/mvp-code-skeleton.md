# Selenium Boot – MVP Code Skeleton

This document defines the **initial code skeleton** for Selenium Boot.
It specifies packages, core classes, and responsibilities **without implementation details**.

The goal is to lock structure before writing logic.

---

## Package Structure (Framework)

```
com.seleniumboot
├── config
│   ├── SeleniumBootConfig.java
│   └── ConfigurationLoader.java
│
├── driver
│   ├── DriverManager.java
│   ├── DriverProvider.java
│   └── DriverContext.java
│
├── execution
│   ├── ExecutionEngine.java
│   ├── ExecutionContext.java
│   └── ParallelExecutionManager.java
│
├── listeners
│   ├── TestExecutionListener.java
│   ├── SuiteExecutionListener.java
│   └── RetryListener.java
│
├── wait
│   ├── WaitEngine.java
│   └── WaitConditions.java
│
├── reporting
│   ├── ReportManager.java
│   └── ReportLifecycle.java
│
├── lifecycle
│   ├── FrameworkBootstrap.java
│   └── FrameworkShutdown.java
│
├── exceptions
│   ├── ConfigurationException.java
│   ├── DriverException.java
│   └── ExecutionException.java
│
└── internal
    └── SeleniumBootContext.java
```

---

## Base Classes (User-Facing)

```
com.seleniumboot.test
├── BaseTest.java
└── BasePage.java
```

### BaseTest (Abstract)
Responsibilities:
- Provides WebDriver access
- Integrates with TestNG lifecycle
- Exposes safe framework utilities

Rules:
- Users must extend this class
- No driver creation logic allowed

---

### BasePage (Abstract)
Responsibilities:
- Holds WebDriver reference
- Provides common UI utilities
- Enforces no assertion rule

Rules:
- No test assertions
- No driver lifecycle logic

---

## Listener Contracts (TestNG)

### SuiteExecutionListener
Implements:
- ISuiteListener

Responsibilities:
- Framework bootstrap
- Configuration loading
- Parallel execution setup

---

### TestExecutionListener
Implements:
- ITestListener

Responsibilities:
- Driver provisioning per test
- Failure evidence capture
- Reporting hooks

---

### RetryListener
Implements:
- IRetryAnalyzer

Responsibilities:
- Retry decision logic
- Attempt count enforcement
- Failure classification (future)

---

## Core Context Objects

### SeleniumBootContext
Responsibilities:
- Holds immutable execution state
- Stores configuration reference
- Manages framework-wide metadata

Must be:
- Thread-safe
- Read-only after initialization

---

## Driver Layer Contracts

### DriverManager
Responsibilities:
- Create and destroy WebDriver instances
- Enforce ThreadLocal ownership
- Apply configuration constraints

---

### DriverProvider (Interface)
Responsibilities:
- Provide browser-specific drivers
- Encapsulate capability creation

Implementations:
- ChromeDriverProvider
- FirefoxDriverProvider
- EdgeDriverProvider

---

## Execution Layer Contracts

### ExecutionEngine
Responsibilities:
- Coordinate test execution lifecycle
- Apply retries and waits
- Act as central orchestrator

---

### ParallelExecutionManager
Responsibilities:
- Configure TestNG thread model
- Validate thread safety constraints

---

## What Is Explicitly Excluded (MVP)

- Remote execution grids
- Plugin system
- Advanced observability
- Distributed execution
- Cloud provider integrations

These are deferred by design.

---

## Design Enforcement Rules

- No static WebDriver fields
- No test-owned lifecycle hooks
- No mutable global state
- No hidden Selenium abstractions

Violations are treated as framework errors.

---

## Next Step After Skeleton

Once this structure is agreed upon:

1. Lock public APIs
2. Implement configuration loading
3. Implement DriverManager
4. Wire listeners incrementally
5. Add first green test

---

## Summary

This skeleton establishes **clear ownership boundaries** and **non-negotiable structure**.
Implementation must follow this layout to preserve Selenium Boot’s architectural integrity.
