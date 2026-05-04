---
id: email-verification
title: Email Verification
sidebar_position: 11
---

# Email Verification

Selenium Boot's `mailbox()` API lets you verify that your application sends the right emails — confirmation links, password resets, welcome messages — as part of your UI test flow, with no extra tooling required.

---

## Quick start

```java
// 1. Trigger the email in your app
$(By.id("email")).type("user@test.com");
$(By.id("register")).click();

// 2. Wait for it and assert
Email email = mailbox().waitForEmail(to("user@test.com").timeout(30));
email.assertSubject("Verify your account");
email.assertBodyContains("welcome to");

// 3. Extract the verification link and follow it
String verifyLink = email.extractLink("Verify Email");
open(verifyLink);
assertThat(By.id("success-msg")).hasText("Email verified!");
```

The `to()` shorthand is available directly in `BaseTest` and `BaseJUnit5Test` — no static import needed.

---

## Backends

Configure the backend once in `selenium-boot.yml`. Test code is identical regardless of which backend you use.

| Provider | Best for |
|---|---|
| `mailhog` | Local dev, Docker CI |
| `mailtrap` | Shared team inboxes, staging |
| `outlook` | Office 365 / corporate email |
| `imap` | Gmail (app password), Yahoo, any IMAP server |

---

## Mailhog (local / Docker)

Run Mailhog alongside your app in Docker:

```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Configure Selenium Boot to use it:

```yaml title="selenium-boot.yml"
email:
  provider: mailhog
  mailhog:
    host: localhost
    port: 8025
```

Point your application's SMTP to `localhost:1025`. Mailhog captures all outgoing mail and serves it via its HTTP API — no real email is sent.

---

## Mailtrap (hosted sandbox)

```yaml title="selenium-boot.yml"
email:
  provider: mailtrap
  mailtrap:
    apiToken:  ${MAILTRAP_TOKEN}
    accountId: ${MAILTRAP_ACCOUNT_ID}
    inboxId:   ${MAILTRAP_INBOX_ID}
```

Get your credentials from [mailtrap.io](https://mailtrap.io) → Email Testing → Inboxes → API Credentials.

---

## Outlook / Office 365

Uses the **Microsoft Graph API** with app-only OAuth2 client credentials — no user sign-in required. Works with Office 365 and personal Outlook accounts.

### Azure AD setup

1. Go to [portal.azure.com](https://portal.azure.com) → **App registrations** → **New registration**
2. **API permissions** → Add → Microsoft Graph → Application permissions → `Mail.Read` + `Mail.ReadWrite`
3. Click **Grant admin consent**
4. **Certificates & secrets** → New client secret → copy the value

```yaml title="selenium-boot.yml"
email:
  provider: outlook
  outlook:
    tenantId:     ${AZURE_TENANT_ID}
    clientId:     ${AZURE_CLIENT_ID}
    clientSecret: ${AZURE_CLIENT_SECRET}
    mailbox:      test-inbox@yourcompany.com
```

The OAuth2 access token is fetched automatically and refreshed before it expires — no manual token management.

---

## IMAP (Gmail, Yahoo, corporate)

Add the optional Jakarta Mail dependency to your project:

```xml title="pom.xml"
<dependency>
  <groupId>com.sun.mail</groupId>
  <artifactId>jakarta.mail</artifactId>
  <version>2.0.1</version>
  <scope>test</scope>
</dependency>
```

```yaml title="selenium-boot.yml"
email:
  provider: imap
  imap:
    host:     imap.gmail.com
    port:     993
    ssl:      true
    username: ${EMAIL_USER}
    password: ${EMAIL_PASS}   # Gmail: use an App Password, not your account password
```

:::tip Gmail App Passwords
Go to [myaccount.google.com](https://myaccount.google.com) → Security → 2-Step Verification → App passwords. Generate one for "Mail" and use it as `EMAIL_PASS`.
:::

---

## Matching criteria

```java
// Match by recipient (most common)
mailbox().waitForEmail(to("user@example.com"));

// Match by recipient + subject
mailbox().waitForEmail(to("user@example.com").subject("Welcome!"));

// Match by body content
mailbox().waitForEmail(to("user@example.com").containing("verify your account"));

// Subject-only (no recipient filter)
mailbox().waitForEmail(any().subject("Password Reset"));

// Custom timeout
mailbox().waitForEmail(to("user@example.com").timeout(60));
```

---

## Email assertions and link extraction

```java
Email email = mailbox().waitForEmail(to("user@example.com"));

// Assert subject
email.assertSubject("Verify your account");

// Assert body content (checks plain-text and HTML body)
email.assertBodyContains("Click the link below");

// Access raw fields
String subject  = email.subject();
String from     = email.from();
String body     = email.body();     // plain text
String htmlBody = email.htmlBody(); // HTML

// Extract the href of an anchor by its visible link text
String link = email.extractLink("Verify Email");
open(link);
```

`extractLink` finds the first `<a>` tag whose visible text exactly matches (case-insensitive). It throws `AssertionError` if no matching anchor is found.

---

## Clearing the inbox

Call `mailbox().clear()` in `@BeforeMethod` to ensure a clean inbox before each test:

```java
@BeforeMethod
public void cleanInbox() {
    mailbox().clear();
}
```

Or enable automatic clearing in config:

```yaml title="selenium-boot.yml"
email:
  autoClear: true   # clear inbox before every test automatically
```

---

## Full config reference

```yaml title="selenium-boot.yml"
email:
  provider: mailhog        # mailhog | mailtrap | outlook | imap
  timeoutSeconds: 30       # default wait timeout
  pollIntervalMs: 1000     # how often to poll the inbox
  autoClear: false         # clear inbox before each test

  mailhog:
    host: localhost
    port: 8025

  mailtrap:
    apiToken:  ${MAILTRAP_TOKEN}
    accountId: ${MAILTRAP_ACCOUNT_ID}
    inboxId:   ${MAILTRAP_INBOX_ID}

  outlook:
    tenantId:     ${AZURE_TENANT_ID}
    clientId:     ${AZURE_CLIENT_ID}
    clientSecret: ${AZURE_CLIENT_SECRET}
    mailbox:      test-inbox@yourcompany.com

  imap:
    host:     imap.gmail.com
    port:     993
    ssl:      true
    username: ${EMAIL_USER}
    password: ${EMAIL_PASS}
    folder:   INBOX
```
