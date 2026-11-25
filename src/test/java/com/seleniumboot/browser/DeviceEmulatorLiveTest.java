package com.seleniumboot.browser;

import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * Live check of the CDP path of {@link DeviceEmulator} on a real Chrome (#289).
 * Skips when Chrome or its driver is not available.
 */
public class DeviceEmulatorLiveTest {

    private static final String PAGE = "data:text/html,<html><head>"
            + "<meta name='viewport' content='width=device-width'></head><body>device</body></html>";

    private WebDriver driver;

    @BeforeClass
    public void start() {
        try {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox", "--window-size=1280,800");
            driver = new ChromeDriver(o);
        } catch (Throwable t) {
            driver = null;
        }
    }

    @AfterMethod(alwaysRun = true)
    public void popDriver() {
        if (driver != null) DriverManager.popSessionOverride();
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        if (driver != null) driver.quit();
    }

    @Test
    public void emulate_appliesViewportScaleAndUserAgent() {
        JavascriptExecutor js = open();

        DeviceEmulator.emulate(new DeviceProfile("Live Phone", 390, 844, 3.0, true, "SeleniumBootLiveUA/1.0"));
        driver.get(PAGE);

        assertEquals(intOf(js, "window.innerWidth"), 390);
        assertEquals(((Number) js.executeScript("return window.devicePixelRatio;")).doubleValue(), 3.0);
        assertEquals(js.executeScript("return navigator.userAgent;"), "SeleniumBootLiveUA/1.0");
    }

    @Test(dependsOnMethods = "emulate_appliesViewportScaleAndUserAgent")
    public void emulate_again_switchesToTheNewProfile() {
        JavascriptExecutor js = open();

        DeviceEmulator.emulate(new DeviceProfile("Live Tablet", 768, 1024, 2.0, true, "SeleniumBootLiveUA/2.0"));
        driver.get(PAGE);

        assertEquals(intOf(js, "window.innerWidth"), 768);
        assertEquals(js.executeScript("return navigator.userAgent;"), "SeleniumBootLiveUA/2.0");
    }

    @Test(dependsOnMethods = "emulate_again_switchesToTheNewProfile")
    public void reset_restoresDesktopViewportAndUserAgent() {
        JavascriptExecutor js = open();

        DeviceEmulator.reset();
        driver.get(PAGE);

        assertNotEquals(intOf(js, "window.innerWidth"), 768);
        assertNotEquals(js.executeScript("return navigator.userAgent;"), "SeleniumBootLiveUA/2.0");
    }

    private JavascriptExecutor open() {
        if (driver == null) throw new SkipException("Chrome not available");
        DriverManager.pushSessionOverride(driver);
        driver.get(PAGE);
        return (JavascriptExecutor) driver;
    }

    private static int intOf(JavascriptExecutor js, String expr) {
        return ((Number) js.executeScript("return " + expr + ";")).intValue();
    }
}
