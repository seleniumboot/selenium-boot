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
 * Creates a {@link RemoteWebDriver} session against Sauce Labs.
 *
 * <p>Uses the W3C {@code sauce:options} extension capability. All existing
 * framework features (retry, reporting, screenshots, parallel) work unchanged.
 *
 * <pre>
 * execution:
 *   mode: saucelabs
 *   saucelabs:
 *     username:      ${SAUCE_USER}
 *     accessKey:     ${SAUCE_KEY}
 *     region:        us-west-1    # us-west-1 | eu-central | apac-southeast
 *     platformName:  "Windows 11"
 *     browser:       chrome
 *     browserVersion: latest
 *     capabilities:              # raw sauce:options overrides
 *       tags: ["regression"]
 * </pre>
 */
public class SauceLabsProvider implements DriverProvider {

    public static final String SESSION_URL_PREFIX = "https://app.saucelabs.com/tests/";

    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
        SeleniumBootConfig.Execution.SauceLabs sl = cfg.getExecution().getSaucelabs();

        String username  = BrowserStackProvider.resolveEnv(sl.getUsername());
        String accessKey = BrowserStackProvider.resolveEnv(sl.getAccessKey());
        String region    = sl.getRegion() != null ? sl.getRegion() : "us-west-1";
        String browser   = sl.getBrowser() != null ? sl.getBrowser() : "chrome";
        String testId    = SeleniumBootContext.getCurrentTestId();

        if (isBlank(username))  throw new IllegalStateException("[SauceLabs] execution.saucelabs.username is required");
        if (isBlank(accessKey)) throw new IllegalStateException("[SauceLabs] execution.saucelabs.accessKey is required");

        Map<String, Object> sauceOptions = new LinkedHashMap<>();
        sauceOptions.put("username",   username);
        sauceOptions.put("accessKey",  accessKey);
        if (!isBlank(testId)) sauceOptions.put("name", testId);
        // User-defined sauce:options overrides
        if (sl.getCapabilities() != null) sauceOptions.putAll(sl.getCapabilities());

        AbstractDriverOptions<?> options = buildOptions(
                browser, sl.getBrowserVersion(), sl.getPlatformName(), sauceOptions);

        String hubUrl = "https://ondemand." + region + ".saucelabs.com:443/wd/hub";

        try {
            WebDriver driver = new RemoteWebDriver(new URL(hubUrl), options);
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(cfg.getTimeouts().getPageLoad()));
            return driver;
        } catch (Exception e) {
            throw new IllegalStateException("[SauceLabs] Failed to create session: " + e.getMessage(), e);
        }
    }

    private static AbstractDriverOptions<?> buildOptions(
            String browser, String browserVersion, String platformName,
            Map<String, Object> sauceOptions) {
        switch (browser.toLowerCase()) {
            case "firefox": {
                FirefoxOptions o = new FirefoxOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                if (!isBlank(platformName))   o.setPlatformName(platformName);
                o.setCapability("sauce:options", sauceOptions);
                return o;
            }
            case "edge": {
                EdgeOptions o = new EdgeOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                if (!isBlank(platformName))   o.setPlatformName(platformName);
                o.setCapability("sauce:options", sauceOptions);
                return o;
            }
            case "safari": {
                SafariOptions o = new SafariOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                if (!isBlank(platformName))   o.setPlatformName(platformName);
                o.setCapability("sauce:options", sauceOptions);
                return o;
            }
            default: {
                ChromeOptions o = new ChromeOptions();
                if (!isBlank(browserVersion)) o.setBrowserVersion(browserVersion);
                if (!isBlank(platformName))   o.setPlatformName(platformName);
                o.setCapability("sauce:options", sauceOptions);
                return o;
            }
        }
    }

    public static String hubUrl(String region) {
        return "https://ondemand." + region + ".saucelabs.com:443/wd/hub";
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
