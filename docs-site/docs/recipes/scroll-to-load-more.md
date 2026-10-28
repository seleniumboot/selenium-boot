---
description: "Test infinite scroll in Selenium: scroll to the bottom, then assert the new items appear with a retrying count check instead of Thread.sleep."
id: scroll-to-load-more
title: Scroll to load more
sidebar_label: Scroll to load more
---

# Scroll to load more

Infinite-scroll pages fetch the next batch only after you reach the bottom, and the new items arrive a moment later. A `Thread.sleep` is either too short (flaky) or too long (slow). Scroll with `BasePage`, then assert on the item count. `assertThat(...).count(n)` retries until it matches or `timeouts.explicit` runs out.

```java title="FeedPage.java"
import com.seleniumboot.test.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static com.seleniumboot.assertion.SeleniumAssert.assertThat;

public class FeedPage extends BasePage {

    private static final By ITEMS = By.cssSelector("#feed .item");

    public FeedPage(WebDriver driver) {
        super(driver);
    }

    public int itemCount() {
        return driver.findElements(ITEMS).size();
    }

    public void loadMore(int batchSize) {
        int before = itemCount();
        scrollToBottom();
        assertThat(ITEMS).count(before + batchSize);   // retries until the batch lands
    }
}
```

```java
feed.loadMore(10);   // 10 -> 20 items
feed.loadMore(10);   // 20 -> 30 items
```

Record the count **before** you scroll, so the assertion is relative to what was there. That keeps it correct no matter how many times you call it.

If the loader is triggered by a specific element (a "sentinel" or the last row) rather than the page bottom, use `scrollTo(By)` on that element instead of `scrollToBottom()`. It scrolls the element into view.

**Deeper reference:** [BasePage](/docs/guides/base-page): the page-object base class with the scroll helpers.
