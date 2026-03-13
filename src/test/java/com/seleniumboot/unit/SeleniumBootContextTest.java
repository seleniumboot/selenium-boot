package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumBootContext}.
 * Uses reflection to reset the static AtomicReference between tests.
 */
public class SeleniumBootContextTest {

    @AfterMethod
    public void resetContext() throws Exception {
        Field configField = SeleniumBootContext.class.getDeclaredField("CONFIG");
        configField.setAccessible(true);
        AtomicReference<?> ref = (AtomicReference<?>) configField.get(null);
        ref.set(null);
        SeleniumBootContext.clearCurrentTestId();
    }

    // ----------------------------------------------------------
    // initialize / isInitialized
    // ----------------------------------------------------------

    @Test
    public void isInitialized_beforeInit_returnsFalse() {
        assertFalse(SeleniumBootContext.isInitialized());
    }

    @Test
    public void initialize_setsConfigAndMarkInitialized() {
        SeleniumBootConfig config = minimalConfig();
        SeleniumBootContext.initialize(config);

        assertTrue(SeleniumBootContext.isInitialized());
        assertSame(config, SeleniumBootContext.getConfig());
    }

    @Test
    public void initialize_calledTwice_firstConfigWins() {
        SeleniumBootConfig first = minimalConfig();
        SeleniumBootConfig second = minimalConfig();
        second.getBrowser().setName("firefox");

        SeleniumBootContext.initialize(first);
        SeleniumBootContext.initialize(second); // should be ignored

        assertSame(first, SeleniumBootContext.getConfig());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void initialize_withNull_throwsIllegalArgument() {
        SeleniumBootContext.initialize(null);
    }

    // ----------------------------------------------------------
    // getConfig — uninitialized guard
    // ----------------------------------------------------------

    @Test(expectedExceptions = IllegalStateException.class)
    public void getConfig_whenNotInitialized_throwsIllegalState() {
        SeleniumBootContext.getConfig();
    }

    // ----------------------------------------------------------
    // Thread-local test ID
    // ----------------------------------------------------------

    @Test
    public void setAndGetCurrentTestId_returnsSameValue() {
        SeleniumBootContext.setCurrentTestId("my.test.Method");
        assertEquals("my.test.Method", SeleniumBootContext.getCurrentTestId());
    }

    @Test
    public void clearCurrentTestId_removesValue() {
        SeleniumBootContext.setCurrentTestId("to-be-cleared");
        SeleniumBootContext.clearCurrentTestId();
        assertNull(SeleniumBootContext.getCurrentTestId());
    }

    @Test
    public void getCurrentTestId_whenNotSet_returnsNull() {
        assertNull(SeleniumBootContext.getCurrentTestId());
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private static SeleniumBootConfig minimalConfig() {
        SeleniumBootConfig config = new SeleniumBootConfig();

        SeleniumBootConfig.Browser browser = new SeleniumBootConfig.Browser();
        browser.setName("chrome");
        config.setBrowser(browser);

        SeleniumBootConfig.Execution execution = new SeleniumBootConfig.Execution();
        execution.setMode("local");
        execution.setBaseUrl("https://example.com");
        execution.setMaxActiveSessions(5);
        config.setExecution(execution);

        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(10);
        timeouts.setPageLoad(30);
        config.setTimeouts(timeouts);

        return config;
    }
}
