# Selenium Boot – Architecture Overview

This document describes the high-level architecture of Selenium Boot, including its core components, design boundaries, and execution flow.

The architecture is intentionally simple, opinionated, and extensible only at well-defined points.

---

## Architectural Goals

The architecture of Selenium Boot is designed to:

- Minimize Selenium framework boilerplate
- Enforce consistent usage patterns
- Reduce flakiness through standardized execution behavior
- Remain transparent to Selenium APIs
- Support long-lived enterprise test suites
- Enable controlled extensibility without fragmentation

---

## High-Level Architecture

Selenium Boot follows a layered, responsibility-driven architecture.

Test Layer  
↑  
Selenium Boot Core  
↑  
Infrastructure Layer  
↑  
Selenium (WebDriver APIs)

---

## Layer Responsibilities

### 1. Test Layer (User-Owned)

Responsibilities:
- TestNG test classes
- Page Object Models
- Test data and assertions
- Business-level test logic

Rules:
- No direct WebDriver setup
- No lifecycle or execution control logic
- Selenium APIs remain directly usable

---

### 2. Selenium Boot Core (Framework-Owned)

Responsibilities:
- Test lifecycle orchestration
- Driver lifecycle management
- Smart wait handling
- Retry logic
- Parallel execution control
- TestNG listener integration

This layer defines how Selenium is executed, not what is tested.

---

### 3. Infrastructure Layer

Responsibilities:
- WebDriver provisioning
- Configuration loading
- Reporting
- Logging
- Environment awareness

---

### 4. Selenium Layer

Responsibilities:
- Browser automation
- WebDriver APIs
- Browser-level interactions

Selenium Boot does not hide or replace Selenium APIs.

---

## Core Components

### Configuration Manager
- Loads configuration from a single YAML file
- Supports environment-based profiles
- Enforces sane defaults

### Driver Manager
- Automatic driver resolution
- Thread-safe driver lifecycle
- Local and remote execution support

### Execution Engine
- Controls test execution flow
- Handles retries and failures
- Coordinates parallel execution

### Wait Engine
- Centralized explicit waits
- Prevents unsafe implicit waits
- Standardized timeout handling

### Reporting Engine
- Clean HTML reporting
- Screenshot capture on failure
- Execution metadata collection

---

## Execution Flow

1. Configuration is loaded at startup
2. TestNG listeners are initialized
3. Drivers are provisioned
4. Tests execute with standardized waits and retries
5. Failures trigger evidence capture
6. Reports are generated
7. Resources are released

---

## Parallel Execution Model

- Enabled by default
- Thread-safe driver handling
- No shared WebDriver state
- Configuration-driven control

---

## Extension Points (Planned)

- Custom driver providers
- Reporting adapters
- Execution lifecycle hooks
- Configuration overrides

---

## Architectural Constraints

- No static global WebDriver access
- No implicit waits
- No test-managed driver lifecycle
- No hidden Selenium abstractions

Violations are considered design errors.

---

## Design Trade-Offs

- Flexibility traded for predictability
- Cleverness traded for debuggability
- Breadth traded for long-term stability

---

## Summary

Selenium Boot’s architecture prioritizes clarity, discipline, and production readiness.
It standardizes framework concerns while preserving full control over test logic.
