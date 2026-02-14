# Selenium Boot -- Production Feature Tracker

## Overview

This document tracks the current capabilities and future roadmap of the
Selenium Boot framework. It is structured for product-level visibility
and release planning.

------------------------------------------------------------------------

## Core Architecture

ID   Feature                                Status   Priority
  ---- -------------------------------------- -------- ----------
1    Opinionated project structure          Stable   High
2    Convention-over-configuration design   Stable   High
3    Single YAML configuration system       Stable   High
4    Environment profile switching          Stable   High
5    Framework bootstrap lifecycle          Stable   High

------------------------------------------------------------------------

## Configuration System

ID   Feature                                           Status   Priority
  ---- ------------------------------------------------- -------- ----------
6    Strict configuration validation                   Stable   High
7    Parallel mode validation (none/methods/classes)   Stable   High
8    Execution mode validation (local/remote)          Stable   High
9    Thread count safety enforcement                   Stable   High
10   Capability customization via YAML                 Stable   High
11   Browser argument customization via YAML           Stable   High
12   Browser-specific namespace validation             Stable   Medium
13   Max active session configuration                  Stable   High

------------------------------------------------------------------------

## Driver Management

ID   Feature                                Status   Priority
  ---- -------------------------------------- -------- ----------
14   ThreadLocal WebDriver isolation        Stable   High
15   Driver health check mechanism          Stable   High
16   Automatic driver recreation on crash   Stable   High
17   Session tracking via AtomicInteger     Stable   High
18   Fail-fast session limit enforcement    Stable   High

------------------------------------------------------------------------

## Parallel Execution

ID   Feature                                 Status   Priority
  ---- --------------------------------------- -------- ----------
19   Dynamic TestNG parallel configuration   Stable   High
20   Safe per-thread driver lifecycle        Stable   High

------------------------------------------------------------------------

## Metrics & Observability

ID   Feature                             Status   Priority
  ---- ----------------------------------- -------- ----------
21   Per-test execution timing capture   Stable   Medium
22   Suite-level execution summary       Stable   Medium
23   Slowest test detection              Stable   Medium

------------------------------------------------------------------------

## Reporting

ID   Feature                         Status   Priority
  ---- ------------------------------- -------- ----------
24   Screenshot capture on failure   Stable   High

------------------------------------------------------------------------

## Browser Support

ID   Feature                           Status   Priority
  ---- --------------------------------- -------- ----------
25   Local Chrome support              Stable   High
26   Local Firefox support             Stable   High
27   Remote WebDriver (Grid) support   Stable   High

------------------------------------------------------------------------

# Future Roadmap

ID   Feature                                        Status    Priority
  ---- ---------------------------------------------- --------- ----------
28   HTML Reporting Engine (Rich UI)                Planned   High
29   Execution metrics export to JSON               Planned   Medium
30   Blocking session queue instead of fail-fast    Planned   Medium
31   CI metadata integration (build, branch, env)   Planned   High
32   Historical trend tracking dashboard            Planned   Medium
33   Plugin architecture for extensions             Planned   High
34   Cloud provider integration layer               Planned   High

------------------------------------------------------------------------

## Release Strategy Suggestion

### Version 1.0

-   Core architecture
-   Configuration system
-   Driver lifecycle hardening
-   Parallel execution control
-   Metrics foundation

### Version 1.1

-   HTML reporting engine
-   JSON metrics export
-   CI metadata integration

### Version 2.0

-   Plugin ecosystem
-   Cloud provider integrations
-   Historical trend dashboard
