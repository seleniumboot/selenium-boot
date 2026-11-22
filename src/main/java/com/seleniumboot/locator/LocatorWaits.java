package com.seleniumboot.locator;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * State checks and auto-waiting for {@link Locator} terminal actions.
 * Package-private implementation detail of the locator package.
 */
final class LocatorWaits {

    private LocatorWaits() {
    }

    /**
     * Waits for the element to reach the specified {@link State}.
     *
     * <p>Re-resolves from scratch on every poll and retries stale or not-yet-present
     * elements until {@code timeouts.explicit} elapses.
     *
     * @param state target element lifecycle state (visible, hidden, attached, detached)
     * @throws LocatorException if the element does not reach {@code state} within {@code timeouts.explicit}
     */
    static void waitFor(Locator l, State state) {
        if (state == null) {
            state = State.VISIBLE;
        }
        SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
        int timeout = cfg.getTimeouts().getExplicit();
        final State targetState = state;
        try {
            new WebDriverWait(l.driver(), Duration.ofSeconds(timeout))
                    .ignoring(StaleElementReferenceException.class, LocatorException.class)
                    .until(d -> checkState(l, targetState));
        } catch (TimeoutException e) {
            throw new LocatorException("Element did not reach state " + targetState + ": " + l.describe()
                    + " (waited " + timeout + "s)", e);
        }
        if (cfg.getDebug().getSlowMoMs() > 0) slowMo(cfg.getDebug().getSlowMoMs());
    }

    static Boolean checkState(Locator l, State state) {
        switch (state) {
            case VISIBLE: {
                try {
                    WebElement el = LocatorResolution.resolve(l);
                    if (!el.isDisplayed()) return null;
                    SeleniumBootConfig.Debug debug = SeleniumBootContext.getConfig().getDebug();
                    if (debug.isHighlight()) highlight(l, el);
                    return Boolean.TRUE;
                } catch (LocatorException | StaleElementReferenceException e) {
                    return null;
                }
            }
            case HIDDEN: {
                try {
                    WebElement el = LocatorResolution.resolve(l);
                    return !el.isDisplayed() ? Boolean.TRUE : null;
                } catch (LocatorException e) {
                    return Boolean.TRUE;
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
            case ATTACHED: {
                try {
                    WebElement el = LocatorResolution.resolve(l);
                    el.isEnabled();
                    SeleniumBootConfig.Debug debug = SeleniumBootContext.getConfig().getDebug();
                    if (debug.isHighlight()) highlight(l, el);
                    return Boolean.TRUE;
                } catch (LocatorException | StaleElementReferenceException e) {
                    return null;
                }
            }
            case DETACHED: {
                try {
                    LocatorResolution.resolve(l);
                    return null;
                } catch (LocatorException e) {
                    return Boolean.TRUE;
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }
            default:
                throw new IllegalArgumentException("Unsupported state: " + state);
        }
    }

    static <T> T whenVisible(Locator l, Function<WebElement, T> action) {
        return whenReady(l, WebElement::isDisplayed, action);
    }

    /**
     * Polls until an element resolves, satisfies {@code ready}, and {@code action} runs on it —
     * re-resolving from scratch on every poll. A missing element ({@link LocatorException}) and a
     * stale one ({@link StaleElementReferenceException}) are retried until
     * {@code timeouts.explicit} elapses. A locator that never matched then throws
     * {@link LocatorException} (cause: the {@code TimeoutException}); anything else throws the
     * {@code TimeoutException}. The action's
     * result may be {@code null}.
     */
    static <T> T whenReady(Locator l, Predicate<WebElement> ready, Function<WebElement, T> action) {
        SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
        int timeout = cfg.getTimeouts().getExplicit();
        SeleniumBootConfig.Debug debug = cfg.getDebug();
        AtomicReference<T> result = new AtomicReference<>();
        try {
            new WebDriverWait(l.driver(), Duration.ofSeconds(timeout))
                    .ignoring(StaleElementReferenceException.class, LocatorException.class)
                    .until(d -> {
                        WebElement el = LocatorResolution.resolve(l);
                        if (!ready.test(el)) return null;
                        if (debug.isHighlight()) highlight(l, el);
                        result.set(action.apply(el));
                        return Boolean.TRUE;
                    });
        } catch (TimeoutException e) {
            ExecutionMetrics.recordFailingLocator(
                    SeleniumBootContext.getCurrentTestId(), l.describe());
            ExecutionMetrics.recordLocatorUse(
                    SeleniumBootContext.getCurrentTestId(), l.describe(), false);
            // A locator that never matched still surfaces as LocatorException, as it did before
            // the wait existed, so callers catching it keep working.
            if (e.getCause() instanceof LocatorException le) {
                throw new LocatorException(le.getMessage() + " (waited " + timeout + "s)", e);
            }
            throw e;
        }
        ExecutionMetrics.recordLocatorUse(SeleniumBootContext.getCurrentTestId(), l.describe(), true);
        if (debug.getSlowMoMs() > 0) slowMo(debug.getSlowMoMs());
        return result.get();
    }

    /** Debug aid: outlines the element; best-effort, never fails the action. The outline is left in place. */
    static void highlight(Locator l, WebElement el) {
        try {
            ((JavascriptExecutor) l.driver()).executeScript(
                    "arguments[0].style.outline='2px solid red';", el);
        } catch (RuntimeException ignored) {
            // element went stale or the page blocks script — highlighting is cosmetic
        }
    }

    static void slowMo(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
