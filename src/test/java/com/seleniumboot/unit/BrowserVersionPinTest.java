package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/** {@code browser.version} is passed to local driver options only when set. */
public class BrowserVersionPinTest {

    private static void apply(Object options, String version) throws Exception {
        Class<?> pin = Class.forName("com.seleniumboot.driver.BrowserVersionPin");
        var m = pin.getDeclaredMethod("apply", org.openqa.selenium.remote.AbstractDriverOptions.class, String.class);
        m.setAccessible(true);
        m.invoke(null, options, version);
    }

    @Test
    public void chrome_versionSet_isPassedThrough() throws Exception {
        ChromeOptions o = new ChromeOptions();
        apply(o, "120.0.6099.109");
        assertEquals(o.getBrowserVersion(), "120.0.6099.109");
    }

    @Test
    public void firefox_versionSet_isPassedThrough() throws Exception {
        FirefoxOptions o = new FirefoxOptions();
        apply(o, " 121.0 ");
        assertEquals(o.getBrowserVersion(), "121.0");
    }

    @Test
    public void nullOrBlank_leavesVersionUnset() throws Exception {
        ChromeOptions a = new ChromeOptions();
        apply(a, null);
        ChromeOptions b = new ChromeOptions();
        apply(b, "  ");
        assertEquals(a.getBrowserVersion(), new ChromeOptions().getBrowserVersion());
        assertEquals(b.getBrowserVersion(), new ChromeOptions().getBrowserVersion());
    }

    @Test
    public void config_defaultsToNull_andRoundTrips() {
        SeleniumBootConfig.Browser b = new SeleniumBootConfig.Browser();
        assertNull(b.getVersion());
        b.setVersion("120");
        assertEquals(b.getVersion(), "120");
    }
}
