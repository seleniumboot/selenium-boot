---
description: "Test a link that opens in a new tab without hand-rolling window handles: withNewWindow switches to it, runs your checks, closes it and restores the original window."
id: handle-new-windows
title: Handle new windows and tabs
sidebar_label: Handle new windows and tabs
---

# Handle new windows and tabs

A link with `target="_blank"` opens a new tab, and every later driver call still targets the old one until you switch. `BasePage`'s `withNewWindow(opener, body)` does the handle bookkeeping: it runs `opener` (the click), switches to the window that opened, runs `body` there, closes it, and switches back to the original window — even if `body` throws.

```java title="FooterPage.java"
public void verifyTermsOpensInNewTab() {
    withNewWindow(
        () -> click(By.linkText("Terms")),
        () -> assertEquals(driver.getTitle(), "Terms of Service"));
    // back on the original window here
}
```

`withNewTab(opener, body)` is an alias — browsers open both the same way.

If no new window appears within `timeouts.explicit`, a `TimeoutException` is thrown and `body` does not run, so a link that silently fails to open fails the test rather than passing it.
