package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class RemoteDriverProvider implements DriverProvider{
    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();

        String browser = config.getBrowser().getName();
        String gridUrl = config.getExecution().getGridUrl();

        if (gridUrl == null || gridUrl.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid gridUrl: %s", gridUrl));
        }

        BrowserArgumentValidator.validate(browser, config.getBrowser().getArguments());

        CapabilityValidator.validate(browser, config.getBrowser().getCapabilities());

        try {
            URL url = new URL(gridUrl);

            if ("chrome".equalsIgnoreCase(browser)) {
                ChromeOptions options = new ChromeOptions();

                if (config.getBrowser().isHeadless()) {
                    options.addArguments("--headless=new");
                }

                if (config.getBrowser().getArguments() != null) {
                    options.addArguments(config.getBrowser().getArguments());
                }

                if (config.getBrowser().getCapabilities() != null) {
                    config.getBrowser().getCapabilities()
                            .forEach(options::setCapability);
                }

                WebDriver driver = new RemoteWebDriver(url, options);
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));
                return driver;
            }

            if ("firefox".equalsIgnoreCase(browser)) {

                FirefoxOptions options = new FirefoxOptions();

                if (config.getBrowser().isHeadless()) {
                    options.addArguments("-headless");
                }

                if (config.getBrowser().getArguments() != null) {
                    options.addArguments(config.getBrowser().getArguments());
                }

                if (config.getBrowser().getCapabilities() != null) {
                    config.getBrowser().getCapabilities()
                            .forEach(options::setCapability);
                }

                WebDriver driver = new RemoteWebDriver(url, options);
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));
                return driver;
            }
            throw new IllegalStateException("Unsupported browser: " + browser);

        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Grid URL: " + gridUrl, e);
        }
    }
}
