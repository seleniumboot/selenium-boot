package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

public class LocalFirefoxDriverProvider implements DriverProvider{

    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();

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

        WebDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        return driver;
    }
}
