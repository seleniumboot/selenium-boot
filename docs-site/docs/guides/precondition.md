---
id: precondition
title: "@PreCondition"
sidebar_position: 12
---

# @PreCondition

`@PreCondition` eliminates repeated setup boilerplate (like login) by running a named provider method once per thread, caching the session state (cookies + localStorage), and restoring it for subsequent tests automatically.

---

## The problem it solves

Without `@PreCondition`:

```java
@BeforeMethod
public void login() {
    open("/login");
    new LoginPage(getDriver()).login("admin", "secret");
    // runs before EVERY test — slow and fragile
}
```

With `@PreCondition`:

```java
@Test
@PreCondition("loginAsAdmin")
public void viewDashboard() {
    open("/dashboard");  // session already established
}

@Test
@PreCondition("loginAsAdmin")
public void editProfile() {
    open("/profile");    // session restored from cache — no re-login
}
```

---

## Step 1 — Create a condition provider

Extend `BaseConditions` and annotate methods with `@ConditionProvider`:

```java
import com.seleniumboot.precondition.BaseConditions;
import com.seleniumboot.precondition.ConditionProvider;
import org.openqa.selenium.By;

public class AppConditions extends BaseConditions {

    @ConditionProvider("loginAsAdmin")
    public void loginAsAdmin() {
        open("/login");
        type(By.id("username"), "admin");
        type(By.id("password"), "admin123");
        click(By.id("submit"));
    }

    @ConditionProvider("loginAsUser")
    public void loginAsUser() {
        open("/login");
        type(By.id("username"), "testuser");
        type(By.id("password"), "user123");
        click(By.id("submit"));
    }

    @ConditionProvider("acceptCookies")
    public void acceptCookies() {
        open("/");
        click(By.id("accept-all-cookies"));
    }
}
```

---

## Step 2 — Register via Java SPI

Create the SPI file:

```
src/main/resources/META-INF/services/com.seleniumboot.precondition.BaseConditions
```

Contents:

```
com.example.AppConditions
```

---

## Step 3 — Annotate your tests

```java
@Test
@PreCondition("loginAsAdmin")
public void viewDashboard() { ... }

@Test
@PreCondition("loginAsUser")
public void viewProfile() { ... }

// Multiple conditions
@Test
@PreCondition({"loginAsAdmin", "acceptCookies"})
public void adminWithCookies() { ... }
```

---

## How caching works

```
Test 1 — @PreCondition("loginAsAdmin")
  → No cache → runs loginAsAdmin() → caches cookies + localStorage

Test 2 — @PreCondition("loginAsAdmin")
  → Cache hit → restores cookies + localStorage → skips login

Test 3 — @PreCondition("loginAsAdmin") [retry]
  → Cache invalidated on retry → re-runs loginAsAdmin() → re-caches
```

Cache is **per thread** — safe for parallel execution. Each thread maintains its own session cache independently.

---

## Multiple login scenarios

Use different condition names for different roles:

```java
@ConditionProvider("loginAsAdmin")
public void loginAsAdmin() { login("admin", "admin123"); }

@ConditionProvider("loginAsManager")
public void loginAsManager() { login("manager", "mgr456"); }

@ConditionProvider("loginAsReadOnly")
public void loginAsReadOnly() { login("viewer", "view789"); }

private void login(String user, String pass) {
    open("/login");
    type(By.id("username"), user);
    type(By.id("password"), pass);
    click(By.id("submit"));
}
```

---

## Programmatic registration

If you prefer not to use SPI:

```java
import com.seleniumboot.precondition.PreConditionRegistry;

// In a @BeforeSuite or SeleniumBootPlugin.onLoad()
PreConditionRegistry.register(new AppConditions());
```
