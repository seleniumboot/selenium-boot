---
description: "Drag one element onto another in Selenium, including HTML5 drag-and-drop pages that ignore the native gesture: Locator.dragTo handles both."
id: drag-and-drop
title: Drag and drop
sidebar_label: Drag and drop
---

# Drag and drop

`Locator.dragTo(target)` drags one element onto another and drops it there.

```java title="BoardPage.java"
Locator.byText("Card A").dragTo(Locator.ofCss("#done-column"));
```

It tries the native drag gesture first. Pages built on the HTML5 drag-and-drop API
(`draggable="true"` with `dragstart` / `drop` listeners) often ignore that gesture, so when no
`drop` event reaches the target, `dragTo` dispatches the full event sequence itself. Both elements
are waited for and re-resolved like every other `Locator` action, so a slow or re-rendering page is
handled within `timeouts.explicit`; a `LocatorException` is thrown if either element never appears.
