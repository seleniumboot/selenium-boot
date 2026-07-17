# Security Policy

## Reporting a vulnerability

If you believe you've found a security vulnerability in Selenium Boot, please report it
privately — **do not open a public GitHub issue.**

Email **security@seleniumboot.com** with:

- a description of the issue and its impact,
- the version affected (`mvn dependency:tree` or your `pom.xml`),
- steps to reproduce, and a proof of concept if you have one.

You can expect an initial acknowledgement within **72 hours**. We'll keep you updated as
we investigate, and we'll credit you in the release notes once a fix ships (unless you'd
prefer to remain anonymous).

Please give us a reasonable window to release a fix before any public disclosure.

## Supported versions

Selenium Boot is pre-1.0 and moving quickly. Security fixes are applied to the **latest
released version** on [Maven Central](https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot).
Please make sure you're on the latest release before reporting.

## Scope

Selenium Boot is a test-automation framework — it runs as part of your build, driving
browsers you control. The most relevant classes of issue are, for example: unsafe
handling of configuration or test data, dependency vulnerabilities, or anything that
could let untrusted input escalate during a test run. Reports about the framework's own
code and its published artifacts are in scope; issues in Selenium, WebDriver, or browser
binaries themselves should go to their respective projects.
