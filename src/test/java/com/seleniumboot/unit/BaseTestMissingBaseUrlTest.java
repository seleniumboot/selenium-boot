package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.test.BaseTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * {@link BaseTest#open()} / {@link BaseTest#open(String)} without {@code execution.baseUrl}
 * must name the missing key (#77). Fails before the browser is touched, so no driver is needed.
 */
public class BaseTestMissingBaseUrlTest {

    private static class Probe extends BaseTest {
        void doOpen() { open(); }
        void doOpen(String path) { open(path); }
    }

    @AfterMethod
    public void resetContext() throws Exception {
        Field f = SeleniumBootContext.class.getDeclaredField("CONFIG");
        f.setAccessible(true);
        ((AtomicReference<?>) f.get(null)).set(null);
    }

    private void initWithoutBaseUrl() {
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setExecution(new SeleniumBootConfig.Execution());
        SeleniumBootContext.initialize(config);
    }

    @Test
    public void open_withoutBaseUrl_namesTheMissingKey() {
        initWithoutBaseUrl();
        IllegalStateException e = expectThrows(IllegalStateException.class, () -> new Probe().doOpen());
        assertTrue(e.getMessage().contains("execution.baseUrl"), e.getMessage());
        assertTrue(e.getMessage().contains("selenium-boot.yml"), e.getMessage());
    }

    @Test
    public void openPath_withoutBaseUrl_namesTheMissingKey() {
        initWithoutBaseUrl();
        IllegalStateException e = expectThrows(IllegalStateException.class, () -> new Probe().doOpen("/x"));
        assertTrue(e.getMessage().contains("execution.baseUrl"), e.getMessage());
    }
}
