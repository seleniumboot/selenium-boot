package com.seleniumboot.locator;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

/**
 * Bodies of {@link Locator}'s interacting terminal actions (click, type, drag, select, …).
 * The public methods on {@link Locator} stay in place and delegate here, so the public API
 * is unchanged. Package-private implementation detail of the locator package.
 */
final class LocatorActions {

    private LocatorActions() {
    }

    static void click(Locator l) {
        LocatorWaits.whenReady(l, el -> el.isDisplayed() && el.isEnabled(), el -> { el.click(); return null; });
    }

    static void doubleClick(Locator l) {
        LocatorWaits.whenReady(l, el -> el.isDisplayed() && el.isEnabled(), el -> {
            new Actions(l.driver()).doubleClick(el).perform();
            return null;
        });
    }

    static void rightClick(Locator l) {
        LocatorWaits.whenVisible(l, el -> {
            new Actions(l.driver()).contextClick(el).perform();
            return null;
        });
    }

    static void check(Locator l) {
        LocatorWaits.whenReady(l, el -> el.isDisplayed() && el.isEnabled(), el -> {
            if (!Locator.isSelected(el)) {
                el.click();
            }
            return null;
        });
    }

    static void uncheck(Locator l) {
        LocatorWaits.whenReady(l, el -> el.isDisplayed() && el.isEnabled(), el -> {
            if (Locator.isSelected(el)) {
                el.click();
            }
            return null;
        });
    }

    static void type(Locator l, String text) {
        LocatorWaits.whenVisible(l, el -> {
            el.clear();
            el.sendKeys(text);
            return null;
        });
    }

    static void append(Locator l, String text) {
        LocatorWaits.whenVisible(l, el -> { el.sendKeys(text); return null; });
    }

    static void press(Locator l, Keys... keys) {
        LocatorWaits.whenVisible(l, el -> {
            el.sendKeys(keys);
            return null;
        });
    }

    static void focus(Locator l) {
        LocatorWaits.whenVisible(l, el -> {
            ((JavascriptExecutor) l.driver()).executeScript("arguments[0].focus();", el);
            return null;
        });
    }

    static void clear(Locator l) {
        LocatorWaits.whenVisible(l, el -> {
            el.clear();
            return null;
        });
    }

    static void hover(Locator l) {
        LocatorWaits.whenVisible(l, el -> { new Actions(l.driver()).moveToElement(el).perform(); return null; });
    }

    static void scrollIntoView(Locator l) {
        LocatorWaits.whenVisible(l, el -> {
            ((JavascriptExecutor) l.driver()).executeScript("arguments[0].scrollIntoView(true);", el);
            return null;
        });
    }

    static void jsClick(Locator l) {
        LocatorWaits.whenVisible(l, el -> {
            ((JavascriptExecutor) l.driver()).executeScript("arguments[0].click();", el);
            return null;
        });
    }

    static void dragTo(Locator l, Locator target) {
        LocatorWaits.whenVisible(target, dst -> LocatorWaits.whenVisible(l, src -> {
            JavascriptExecutor js = (JavascriptExecutor) l.driver();
            js.executeScript(LocatorJs.ARM_DROP_LISTENER_JS, dst);
            new Actions(l.driver()).dragAndDrop(src, dst).perform();
            if (!Boolean.TRUE.equals(js.executeScript(LocatorJs.COLLECT_DROP_RESULT_JS, dst))) {
                js.executeScript(LocatorJs.SYNTHETIC_DRAG_JS, src, dst);
            }
            return null;
        }));
    }

    static void selectOption(Locator l, String text) {
        LocatorWaits.whenVisible(l, el -> {
            new Select(el).selectByVisibleText(text);
            return null;
        });
    }

    static void selectOptionByValue(Locator l, String value) {
        LocatorWaits.whenVisible(l, el -> {
            new Select(el).selectByValue(value);
            return null;
        });
    }
}
