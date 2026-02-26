package com.seleniumboot.driver;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LocalChromeDriverProvider implements DriverProvider {

    @Override
    public WebDriver createDriver() {
        SeleniumBootConfig config = SeleniumBootContext.getConfig();
        ChromeOptions options = new ChromeOptions();

//        ChromeOption Arguments Validation
        List<String> arguments = config.getBrowser().getArguments();
        BrowserArgumentValidator.validate("chrome", arguments);

//        ChromeDriver Capabilities validation
        Map<String, Object> capabilities = config.getBrowser().getCapabilities();
        CapabilityValidator.validate("chrome", capabilities);

        if (capabilities != null) {
            capabilities.forEach(options::setCapability);
        }

        if (config.getBrowser().isHeadless()) {
            options.addArguments("--headless=new");
        }
        if (arguments != null) {
            options.addArguments(arguments);
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));

        return driver;
    }
}
