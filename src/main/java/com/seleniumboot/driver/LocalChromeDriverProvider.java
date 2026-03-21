package com.seleniumboot.driver;

import com.seleniumboot.browser.DownloadManager;
import com.seleniumboot.ci.CiEnvironmentDetector;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.HashMap;
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

        // Docker/container: Chrome requires these flags to run without a real display
        if (CiEnvironmentDetector.isContainer()) {
            options.addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--window-size=1920,1080"
            );
        }

        if (arguments != null) {
            options.addArguments(arguments);
        }

        // Auto-configure download directory — local mode only.
        // Remote/Grid sessions download to the node's filesystem; DownloadManager
        // polls the local filesystem, so prefs would be ineffective in remote mode.
        if (!"remote".equalsIgnoreCase(config.getExecution().getMode())) {
            String downloadDir = DownloadManager.resolveDownloadDir().getAbsolutePath();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            options.setExperimentalOption("prefs", prefs);
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));

        return driver;
    }
}
