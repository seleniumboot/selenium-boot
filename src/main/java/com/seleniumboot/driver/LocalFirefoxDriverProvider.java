package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LocalFirefoxDriverProvider implements DriverProvider{

    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();

        FirefoxOptions options = new FirefoxOptions();
        List<String> arguments = config.getBrowser().getArguments();

        BrowserArgumentValidator.validate("firefox", arguments);

        if (arguments != null) {
            options.addArguments(arguments);
        }

        Map<String, Object> capabilities = config.getBrowser().getCapabilities();
        CapabilityValidator.validate("firefox", capabilities);

        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }

        if (config.getBrowser().isHeadless()) {
            options.addArguments("-headless");
        }

        WebDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        return driver;
    }
}
