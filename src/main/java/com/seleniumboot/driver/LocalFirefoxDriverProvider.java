package com.seleniumboot.driver;

import com.seleniumboot.browser.DownloadManager;
import com.seleniumboot.ci.CiEnvironmentDetector;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

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

        // Docker/container: set explicit window size for consistent rendering
        if (CiEnvironmentDetector.isContainer()) {
            options.addArguments("--width=1920", "--height=1080");
        }

        // Auto-configure download directory — local mode only.
        // Remote/Grid sessions download to the node's filesystem; DownloadManager
        // polls the local filesystem, so prefs would be ineffective in remote mode.
        if (!"remote".equalsIgnoreCase(config.getExecution().getMode())) {
            String downloadDir = DownloadManager.resolveDownloadDir().getAbsolutePath();
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.dir", downloadDir);
            profile.setPreference("browser.download.useDownloadDir", true);
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/pdf,text/csv,text/plain," +
                "application/zip,application/x-zip-compressed,application/json");
            profile.setPreference("pdfjs.disabled", true);
            options.setProfile(profile);
        }

        WebDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));

        return driver;
    }
}
