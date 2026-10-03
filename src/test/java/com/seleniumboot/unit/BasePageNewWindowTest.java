package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.test.BasePage;
import org.mockito.MockedStatic;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/** {@code withNewWindow} / {@code withNewTab}: switch to the opened window, always restore the original. */
public class BasePageNewWindowTest {

    private WebDriver driver;
    private WebDriver.TargetLocator target;
    private MockedStatic<SeleniumBootContext> contextMock;
    private Page page;

    private static final class Page extends BasePage {
        Page(WebDriver d) { super(d); }
        void window(Runnable o, Runnable b) { withNewWindow(o, b); }
        void tab(Runnable o, Runnable b) { withNewTab(o, b); }
    }

    @BeforeMethod
    public void setup() {
        driver = mock(WebDriver.class);
        target = mock(WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(target);
        when(driver.getWindowHandle()).thenReturn("main");
        when(driver.getWindowHandles()).thenReturn(Set.of("main"));

        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(1);
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setTimeouts(timeouts);
        contextMock = mockStatic(SeleniumBootContext.class);
        contextMock.when(SeleniumBootContext::getConfig).thenReturn(config);
        page = new Page(driver);
    }

    @AfterMethod
    public void teardown() { contextMock.close(); }

    private void openingWindowAddsHandle() {
        when(driver.getWindowHandles()).thenReturn(Set.of("main")).thenReturn(new LinkedHashSet<>(List.of("main", "popup")));
    }

    @Test
    public void newWindow_switchesRunsBodyClosesItAndRestoresOriginal() {
        openingWindowAddsHandle();
        AtomicBoolean ran = new AtomicBoolean();

        page.window(() -> {}, () -> {
            ran.set(true);
            verify(target, atLeastOnce()).window("popup");
        });

        assertTrue(ran.get());
        verify(driver).close();
        verify(target, atLeastOnce()).window("main");
    }

    @Test
    public void withNewTab_behavesTheSame() {
        openingWindowAddsHandle();
        page.tab(() -> {}, () -> {});
        verify(target, atLeastOnce()).window("popup");
        verify(target, atLeastOnce()).window("main");
    }

    @Test
    public void noNewWindow_failsLoudlyWithoutRunningBodyAndStaysOnOriginal() {
        AtomicBoolean ran = new AtomicBoolean();

        assertThrows(TimeoutException.class, () -> page.window(() -> {}, () -> ran.set(true)));

        assertFalse(ran.get());
        verify(driver, never()).close();
        verify(target).window("main");
    }

    @Test
    public void bodyThrows_stillClosesNewWindowAndRestoresOriginal() {
        openingWindowAddsHandle();

        assertThrows(IllegalStateException.class,
                () -> page.window(() -> {}, () -> { throw new IllegalStateException("boom"); }));

        verify(driver).close();
        verify(target, atLeastOnce()).window("main");
    }

    @Test
    public void openerThrows_restoresOriginal() {
        assertThrows(IllegalStateException.class,
                () -> page.window(() -> { throw new IllegalStateException("click failed"); }, () -> {}));

        verify(target).window("main");
        verify(driver, never()).close();
    }
}
