package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.URL;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a {@link RemoteWebDriver} session against BrowserStack Automate.
 *
 * <p>Uses the W3C {@code bstack:options} extension capability — no credentials
 * in the URL, full Selenium 4 protocol compliance.
 *
 * <pre>
 * execution:
 *   mode: browserstack
 *   browserstack:
 *     username:      ${BS_USER}
 *     accessKey:     ${BS_KEY}
 *     os:            Windows
 *     osVersion:     "11"
 *     browser:       chrome
 *     browserVersion: latest
 *     capabilities:         # raw bstack:options overrides
 *       debug: true
 *       networkLogs: true
 * </pre>
 */
public class BrowserStackProvider implements DriverProvider {

    public static final String HUB_URL = "https://hub.browserstack.com/wd/hub";
    public static final String SESSION_URL_PREFIX = "https://app.browserstack.com/automate/tests/";

    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
        SeleniumBootConfig.Execution.BrowserStack bs = cfg.getExecution().getBrowserstack();

        String username   = resolveEnv(bs.getUsername());
        String accessKey  = resolveEnv(bs.getAccessKey());
        String browser    = bs.getBrowser() != null ? bs.getBrowser() : "chrome";
        String testId     = SeleniumBootContext.getCurrentTestId();

        if (isBlank(username)) throw new IllegalStateException("[BrowserStack] execution.browserstack.username is required");
        if (isBlank(accessKey)) throw new IllegalStateException("[BrowserStack] execution.browserstack.accessKey is required");

        Map<String, Object> bstackOptions = new LinkedHashMap<>();
        bstackOptions.put("userName",   username);
        bstackOptions.put("accessKey",  accessKey);
        if (!isBlank(bs.getOs()))        bstackOptions.put("os",        bs.getOs());
        if (!isBlank(bs.getOsVersion())) bstackOptions.put("osVersion", bs.getOsVersion());
        if (!isBlank(bs.getDevice()))    bstackOptions.put("deviceName", bs.getDevice());
        if (!isBlank(bs.getDevice()))    bstackOptions.put("realMobile", String.valueOf(bs.isRealMobile()));
        if (!isBlank(testId))            bstackOptions.put("sessionName", testId);
        // User-defined bstack:options overrides
        if (bs.getCapabilities() != null) bstackOptions.putAll(bs.getCapabilities());

        AbstractDriverOptions<?> options = buildOptions(browser, bs.getBrowserVersion(), bstackOptions);

        try {
            WebDriver driver = new RemoteWebDriver(new URL(HUB_URL), options);
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(cfg.getTimeouts().getPageLoad()));
            return driver;
        } catch (Exception e) {
            throw new IllegalStateException("[BrowserStack] Failed to create session: " + e.getMessage(), e);
        }
    }

    private static AbstractDriverOptions<?> buildOptions(
            String browser, String browserVersion, Map<String, Object> bstackOptions) {
        switch (browser.toLowerCase()) {
            case "firefox": {
                FirefoxOptions o = new FirefoxOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                o.setCapability("bstack:options", bstackOptions);
                return o;
            }
            case "edge": {
                EdgeOptions o = new EdgeOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                o.setCapability("bstack:options", bstackOptions);
                return o;
            }
            case "safari": {
                SafariOptions o = new SafariOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                o.setCapability("bstack:options", bstackOptions);
                return o;
            }
            default: {
                ChromeOptions o = new ChromeOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                o.setCapability("bstack:options", bstackOptions);
                return o;
            }
        }
    }

    public static String resolveEnv(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String var = value.substring(2, value.length() - 1);
            String resolved = System.getenv(var);
            return resolved != null ? resolved : System.getProperty(var, value);
        }
        return value;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
