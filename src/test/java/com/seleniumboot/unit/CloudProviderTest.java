package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.BrowserStackProvider;
import com.seleniumboot.driver.SauceLabsProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link BrowserStackProvider} and {@link SauceLabsProvider}.
 * Real session creation requires valid cloud credentials and is covered by
 * integration tests in the consumer project.
 */
public class CloudProviderTest {

    // ── BrowserStack ──────────────────────────────────────────────────────

    @Test
    public void browserstack_hubUrl_isCorrect() {
        assertEquals(BrowserStackProvider.HUB_URL, "https://hub.browserstack.com/wd/hub");
    }

    @Test
    public void browserstack_sessionUrlPrefix_isCorrect() {
        assertEquals(BrowserStackProvider.SESSION_URL_PREFIX,
                "https://app.browserstack.com/automate/tests/");
    }

    @Test
    public void browserstack_sessionUrl_appendsSessionId() {
        String url = BrowserStackProvider.SESSION_URL_PREFIX + "abc123";
        assertEquals(url, "https://app.browserstack.com/automate/tests/abc123");
    }

    @Test
    public void browserstack_configDefaults() {
        SeleniumBootConfig.Execution.BrowserStack cfg = new SeleniumBootConfig.Execution.BrowserStack();
        assertEquals(cfg.getBrowser(), "chrome");
        assertEquals(cfg.getBrowserVersion(), "latest");
        assertTrue(cfg.isRealMobile());
        assertNotNull(cfg.getCapabilities());
        assertTrue(cfg.getCapabilities().isEmpty());
    }

    @Test
    public void browserstack_configSetters() {
        SeleniumBootConfig.Execution.BrowserStack cfg = new SeleniumBootConfig.Execution.BrowserStack();
        cfg.setUsername("myUser");
        cfg.setAccessKey("myKey");
        cfg.setOs("Windows");
        cfg.setOsVersion("11");
        cfg.setBrowser("firefox");
        cfg.setBrowserVersion("120.0");
        cfg.setDevice("Samsung Galaxy S22");
        cfg.setRealMobile(false);

        assertEquals(cfg.getUsername(), "myUser");
        assertEquals(cfg.getAccessKey(), "myKey");
        assertEquals(cfg.getOs(), "Windows");
        assertEquals(cfg.getOsVersion(), "11");
        assertEquals(cfg.getBrowser(), "firefox");
        assertEquals(cfg.getBrowserVersion(), "120.0");
        assertEquals(cfg.getDevice(), "Samsung Galaxy S22");
        assertFalse(cfg.isRealMobile());
    }

    @Test
    public void browserstack_envVarResolution_returnsValue() {
        // Resolves ${VAR} pattern using system property fallback
        System.setProperty("TEST_BS_KEY", "resolved-key");
        String result = BrowserStackProvider.resolveEnv("${TEST_BS_KEY}");
        assertEquals(result, "resolved-key");
        System.clearProperty("TEST_BS_KEY");
    }

    @Test
    public void browserstack_envVarResolution_plainValuePassthrough() {
        assertEquals(BrowserStackProvider.resolveEnv("plain-value"), "plain-value");
    }

    @Test
    public void browserstack_envVarResolution_nullSafe() {
        assertNull(BrowserStackProvider.resolveEnv(null));
    }

    // ── Sauce Labs ────────────────────────────────────────────────────────

    @Test
    public void saucelabs_sessionUrlPrefix_isCorrect() {
        assertEquals(SauceLabsProvider.SESSION_URL_PREFIX,
                "https://app.saucelabs.com/tests/");
    }

    @Test
    public void saucelabs_sessionUrl_appendsSessionId() {
        String url = SauceLabsProvider.SESSION_URL_PREFIX + "xyz789";
        assertEquals(url, "https://app.saucelabs.com/tests/xyz789");
    }

    @Test
    public void saucelabs_configDefaults() {
        SeleniumBootConfig.Execution.SauceLabs cfg = new SeleniumBootConfig.Execution.SauceLabs();
        assertEquals(cfg.getRegion(), "us-west-1");
        assertEquals(cfg.getPlatformName(), "Windows 11");
        assertEquals(cfg.getBrowser(), "chrome");
        assertEquals(cfg.getBrowserVersion(), "latest");
        assertNotNull(cfg.getCapabilities());
        assertTrue(cfg.getCapabilities().isEmpty());
    }

    @Test
    public void saucelabs_hubUrl_usWest() {
        assertEquals(SauceLabsProvider.hubUrl("us-west-1"),
                "https://ondemand.us-west-1.saucelabs.com:443/wd/hub");
    }

    @Test
    public void saucelabs_hubUrl_euCentral() {
        assertEquals(SauceLabsProvider.hubUrl("eu-central"),
                "https://ondemand.eu-central.saucelabs.com:443/wd/hub");
    }

    @Test
    public void saucelabs_hubUrl_apacSoutheast() {
        assertEquals(SauceLabsProvider.hubUrl("apac-southeast"),
                "https://ondemand.apac-southeast.saucelabs.com:443/wd/hub");
    }

    @Test
    public void saucelabs_configSetters() {
        SeleniumBootConfig.Execution.SauceLabs cfg = new SeleniumBootConfig.Execution.SauceLabs();
        cfg.setUsername("sauceUser");
        cfg.setAccessKey("sauceKey");
        cfg.setRegion("eu-central");
        cfg.setPlatformName("macOS 13");
        cfg.setBrowser("safari");
        cfg.setBrowserVersion("16.0");

        assertEquals(cfg.getUsername(), "sauceUser");
        assertEquals(cfg.getAccessKey(), "sauceKey");
        assertEquals(cfg.getRegion(), "eu-central");
        assertEquals(cfg.getPlatformName(), "macOS 13");
        assertEquals(cfg.getBrowser(), "safari");
        assertEquals(cfg.getBrowserVersion(), "16.0");
    }
}
