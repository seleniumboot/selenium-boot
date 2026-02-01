# Selenium Boot – CI Execution Contract

This document defines how Selenium Boot is expected to run in CI pipelines.

---

## Execution Command

Standard command:
mvn test

No custom flags required.

---

## CI Assumptions

- Headless execution supported
- Reports generated locally
- Artifacts archived by CI tool

---

## Parallel Execution

- Enabled by default
- Thread count configurable

---

## Failure Handling

- Test failures fail the build
- Reporting failures do not

---

## Summary

CI execution must be predictable and boring.
