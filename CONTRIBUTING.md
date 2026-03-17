# Contributing to Selenium Boot

Thank you for your interest in contributing! Selenium Boot is an opinionated framework — contributions that align with its core philosophy are warmly welcomed.

---

## Philosophy first

Before contributing, please understand the guiding principles:

1. **Zero boilerplate** — if a user needs more than 1 line to enable something, it should be a default
2. **Convention over configuration** — smart defaults, YAML opt-in for advanced behaviour
3. **Single dependency** — adding `selenium-boot` should be all a user needs
4. **Test code stays clean** — internals handle lifecycle; test methods contain only intent

Features that significantly increase complexity without clear user value may be declined. This is intentional — the framework's simplicity is its main feature.

---

## Ways to contribute

- **Bug reports** — open a GitHub Issue with steps to reproduce
- **Bug fixes** — fork, fix, and open a Pull Request
- **Documentation improvements** — typos, missing examples, unclear explanations
- **Feature suggestions** — open a GitHub Discussion before writing code
- **Test coverage** — unit tests for untested code paths are always welcome

---

## Development setup

### Prerequisites

- Java 17+
- Maven 3.8+
- Git

### Clone and build

```bash
git clone https://github.com/seleniumboot/selenium-boot.git
cd selenium-boot
mvn clean verify
```

All unit tests must pass before submitting a PR.

### Test against the consumer project

A working consumer project is available at:
**https://github.com/seleniumboot/selenium-boot-test**

Clone it alongside this repo and update its `pom.xml` to use your local snapshot:

```xml
<dependency>
    <groupId>io.github.seleniumboot</groupId>
    <artifactId>selenium-boot</artifactId>
    <version>YOUR-SNAPSHOT-VERSION</version>
</dependency>
```

Then install your local build:

```bash
mvn clean install -DskipTests
```

And run the consumer project tests to verify end-to-end behaviour.

---

## Submitting a Pull Request

1. **Fork** the repository
2. **Create a branch** from `master`:
   ```bash
   git checkout -b fix/your-fix-description
   ```
3. **Make your changes** — keep commits focused and atomic
4. **Run tests**:
   ```bash
   mvn clean verify
   ```
5. **Open a PR** against `master` with a clear description of what and why

### PR checklist

- [ ] All existing tests pass (`mvn clean verify`)
- [ ] New behaviour is covered by unit tests
- [ ] No new external dependencies introduced without discussion
- [ ] Code follows existing conventions (no new frameworks, minimal abstraction)
- [ ] PR description explains the problem being solved

---

## Reporting bugs

Open an Issue at: https://github.com/seleniumboot/selenium-boot/issues

Include:
- Selenium Boot version
- Java version
- Browser + driver version
- Minimal reproduction steps
- Full stack trace if applicable

---

## Suggesting features

Open a GitHub Discussion before writing any code:
https://github.com/seleniumboot/selenium-boot/discussions

Describe:
- The problem you're solving (not just the solution)
- How it fits the framework philosophy
- Whether it can be opt-in to avoid affecting existing users

---

## Code style

- Follow existing code style — no formatter config is enforced, just be consistent
- Prefer clarity over cleverness
- Avoid adding dependencies — if something can be done with the JDK, do it that way
- Keep public APIs minimal — every public method is a commitment

---

## License

By contributing, you agree that your contributions will be licensed under the [Apache 2.0 License](LICENSE).
