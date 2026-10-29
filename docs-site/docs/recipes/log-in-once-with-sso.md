---
description: "Log in once through OAuth/SSO in Selenium and reuse the session across tests with @PreCondition. What you can automate, what you can't, and where credentials go."
id: log-in-once-with-sso
title: Log in once with OAuth/SSO
sidebar_label: Log in once with SSO
---

# Log in once with OAuth/SSO

An SSO login redirects through the identity provider (IdP) and back, which makes it slow. Doing it before every test multiplies that cost. Do it once per thread with [`@PreCondition`](/docs/guides/precondition): the provider logs in, Selenium Boot caches the session, and later tests restore it.

:::note This is a sketch
The selectors and URLs below are for a generic IdP login form. Swap in your IdP's real ones. It is not run against a public test IdP.
:::

## 1. Keep credentials out of the code

Read them from the environment and fail loudly when they're missing:

```java title="SsoConditions.java"
import com.seleniumboot.precondition.BaseConditions;
import com.seleniumboot.precondition.ConditionProvider;
import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;

public class SsoConditions extends BaseConditions {

    @ConditionProvider("loginViaSso")
    public void loginViaSso() {
        open("/");                                  // app redirects to the IdP
        type(By.id("username"), env("SSO_USER"));
        click(By.id("next"));
        type(By.id("password"), env("SSO_PASSWORD"));
        click(By.id("sign-in"));
        // wait until you're back on the app, signed in
        WaitEngine.waitForVisible(By.id("user-menu"));
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set the " + name + " environment variable");
        }
        return value;
    }
}
```

Register it like any other provider (SPI file or `PreConditionRegistry.register(...)`, see the [@PreCondition guide](/docs/guides/precondition)). Use a dedicated test account, never a real user's.

## 2. Use it in tests

```java
@Test
@PreCondition("loginViaSso")
public void viewDashboard() {
    open("/dashboard");   // signed in, no IdP round trip
}
```

The first test on each thread runs the provider. The rest restore the cached cookies and localStorage. A retry runs the provider again.

## What is and isn't automatable

| Step | Automatable? |
|---|---|
| Username/password form on the IdP | Yes, as above |
| MFA (SMS, authenticator app, push) | **No.** Ask for a test account or tenant with MFA disabled, or a service account |
| Third-party consent screens ("Allow this app?") | Only if the screen appears every time and has stable selectors. Pre-approve the app for the test account if you can |
| CAPTCHA / bot detection | **No.** Use an allow-listed test tenant |

## Limits to know

- The cache holds **cookies and localStorage** only. If your app keeps its token in `sessionStorage`, it is not restored.
- The cache is **per thread** and lives for the run. A session that expires mid-run is not refreshed, so keep the run shorter than the session lifetime.
- It is not shared across JVMs, so each parallel thread logs in once.

**Deeper reference:** [@PreCondition](/docs/guides/precondition): providers, caching and retry behaviour.
