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

        try {
            URL url = new URL(gridUrl);

            if ("chrome".equalsIgnoreCase(browser)) {
                ChromeOptions options = new ChromeOptions();

                if (config.getBrowser().isHeadless()) {
                    options.addArguments("--headless=new");
                }

                WebDriver driver = new RemoteWebDriver(url, options);
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                return driver;
            }

            if ("firefox".equalsIgnoreCase(browser)) {

                FirefoxOptions options = new FirefoxOptions();

                if (config.getBrowser().isHeadless()) {
                    options.addArguments("-headless");
                }

                WebDriver driver = new RemoteWebDriver(url, options);
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                return driver;
            }
            throw new IllegalStateException("Unsupported browser: " + browser);

        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Grid URL: " + gridUrl, e);
        }
    }
}
