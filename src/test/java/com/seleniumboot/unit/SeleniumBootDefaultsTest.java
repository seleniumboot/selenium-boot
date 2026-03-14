package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.config.SeleniumBootDefaults;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumBootDefaults}.
 */
public class SeleniumBootDefaultsTest {

    @AfterMethod
    public void reset() {
        SeleniumBootDefaults.reset();
    }

    // ----------------------------------------------------------
    // set / get
    // ----------------------------------------------------------

    @Test
    public void set_storesValueRetrievableByGet() {
        SeleniumBootDefaults.set("browser.name", "edge");
        assertEquals(SeleniumBootDefaults.get("browser.name"), "edge");
    }

    @Test
    public void get_unknownKey_returnsNull() {
        assertNull(SeleniumBootDefaults.get("nonexistent.key"));
    }

    @Test
    public void reset_clearsAllOverrides() {
        SeleniumBootDefaults.set("browser.name", "edge");
        SeleniumBootDefaults.reset();
        assertNull(SeleniumBootDefaults.get("browser.name"));
    }

    // ----------------------------------------------------------
    // applyMissing — browser.name
    // ----------------------------------------------------------

    @Test
    public void applyMissing_browserName_appliedWhenNull() {
        SeleniumBootDefaults.set("browser.name", "edge");
        SeleniumBootConfig config = configWithNullBrowserName();

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getBrowser().getName(), "edge");
    }

    @Test
    public void applyMissing_browserName_notOverriddenWhenAlreadySet() {
        SeleniumBootDefaults.set("browser.name", "edge");
        SeleniumBootConfig config = configWithBrowserName("chrome");

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getBrowser().getName(), "chrome",
            "YAML value should win over default");
    }

    // ----------------------------------------------------------
    // applyMissing — timeouts
    // ----------------------------------------------------------

    @Test
    public void applyMissing_explicitTimeout_appliedWhenZero() {
        SeleniumBootDefaults.set("timeouts.explicit", 20);
        SeleniumBootConfig config = configWithTimeouts(0, 0);

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getTimeouts().getExplicit(), 20);
    }

    @Test
    public void applyMissing_explicitTimeout_notOverriddenWhenSet() {
        SeleniumBootDefaults.set("timeouts.explicit", 20);
        SeleniumBootConfig config = configWithTimeouts(10, 30);

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getTimeouts().getExplicit(), 10,
            "YAML value should win over default");
    }

    @Test
    public void applyMissing_pageLoadTimeout_appliedWhenZero() {
        SeleniumBootDefaults.set("timeouts.pageLoad", 60);
        SeleniumBootConfig config = configWithTimeouts(0, 0);

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getTimeouts().getPageLoad(), 60);
    }

    // ----------------------------------------------------------
    // applyMissing — execution
    // ----------------------------------------------------------

    @Test
    public void applyMissing_maxActiveSessions_appliedWhenZero() {
        SeleniumBootDefaults.set("execution.maxActiveSessions", 8);
        SeleniumBootConfig config = configWithExecution(0, 0);

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getExecution().getMaxActiveSessions(), 8);
    }

    @Test
    public void applyMissing_threadCount_appliedWhenZero() {
        SeleniumBootDefaults.set("execution.threadCount", 4);
        SeleniumBootConfig config = configWithExecution(0, 0);

        SeleniumBootDefaults.applyMissing(config);

        assertEquals(config.getExecution().getThreadCount(), 4);
    }

    // ----------------------------------------------------------
    // applyMissing — null-safety
    // ----------------------------------------------------------

    @Test
    public void applyMissing_nullBrowserSection_doesNotThrow() {
        SeleniumBootDefaults.set("browser.name", "edge");
        SeleniumBootConfig config = new SeleniumBootConfig(); // browser section is null

        SeleniumBootDefaults.applyMissing(config); // must not throw
    }

    @Test
    public void applyMissing_nullTimeoutsSection_doesNotThrow() {
        SeleniumBootDefaults.set("timeouts.explicit", 10);
        SeleniumBootConfig config = new SeleniumBootConfig();

        SeleniumBootDefaults.applyMissing(config); // must not throw
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private static SeleniumBootConfig configWithNullBrowserName() {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Browser browser = new SeleniumBootConfig.Browser();
        // name left null
        config.setBrowser(browser);
        return config;
    }

    private static SeleniumBootConfig configWithBrowserName(String name) {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Browser browser = new SeleniumBootConfig.Browser();
        browser.setName(name);
        config.setBrowser(browser);
        return config;
    }

    private static SeleniumBootConfig configWithTimeouts(int explicit, int pageLoad) {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(explicit);
        timeouts.setPageLoad(pageLoad);
        config.setTimeouts(timeouts);
        return config;
    }

    private static SeleniumBootConfig configWithExecution(int maxSessions, int threadCount) {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Execution execution = new SeleniumBootConfig.Execution();
        execution.setMaxActiveSessions(maxSessions);
        execution.setThreadCount(threadCount);
        config.setExecution(execution);
        return config;
    }
}
