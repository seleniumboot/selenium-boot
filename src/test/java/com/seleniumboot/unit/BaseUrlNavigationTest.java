package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.BaseUrlNavigation;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.junit5.BaseJUnit5Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/** Unreachable / missing {@code execution.baseUrl} must be reported in terms of the config key. */
public class BaseUrlNavigationTest {

    @AfterMethod
    public void resetContext() throws Exception {
        Field f = SeleniumBootContext.class.getDeclaredField("CONFIG");
        f.setAccessible(true);
        ((AtomicReference<?>) f.get(null)).set(null);
    }

    private static WebDriver failingWith(String message) {
        WebDriver driver = mock(WebDriver.class);
        doThrow(new WebDriverException(message)).when(driver).get(anyString());
        return driver;
    }

    @Test
    public void chromeDnsFailure_namesUrlAndConfigKey() {
        WebDriver driver = failingWith("unknown error: net::ERR_NAME_NOT_RESOLVED (Session info: chrome=153)");
        WebDriverException e = expectThrows(WebDriverException.class,
                () -> BaseUrlNavigation.go(driver, "https://nope.invalid"));
        assertTrue(e.getMessage().contains("https://nope.invalid"), e.getMessage());
        assertTrue(e.getMessage().contains("execution.baseUrl"), e.getMessage());
        assertTrue(e.getMessage().contains("ERR_NAME_NOT_RESOLVED"), e.getMessage());
        assertNotNull(e.getCause(), "original driver error must stay as the cause");
    }

    @Test
    public void firefoxDnsFailure_isRecognised() {
        WebDriver driver = failingWith("Reached error page: about:neterror?e=dnsNotFound&u=https%3A//x");
        WebDriverException e = expectThrows(WebDriverException.class,
                () -> BaseUrlNavigation.go(driver, "https://x"));
        assertTrue(e.getMessage().contains("execution.baseUrl"), e.getMessage());
    }

    @Test
    public void unrelatedDriverError_isRethrownUntouched() {
        WebDriver driver = failingWith("invalid session id");
        WebDriverException e = expectThrows(WebDriverException.class,
                () -> BaseUrlNavigation.go(driver, "https://example.com"));
        assertTrue(e.getMessage().startsWith("invalid session id"), e.getMessage());
    }

    @Test
    public void reachableUrl_navigatesNormally() {
        WebDriver driver = mock(WebDriver.class);
        BaseUrlNavigation.go(driver, "https://example.com");
        verify(driver).get("https://example.com");
    }

    @Test
    public void require_rejectsNullEmptyAndBlank() {
        for (String bad : new String[] {null, "", "   "}) {
            IllegalStateException e = expectThrows(IllegalStateException.class, () -> BaseUrlNavigation.require(bad));
            assertTrue(e.getMessage().contains("execution.baseUrl"), e.getMessage());
        }
        assertEquals(BaseUrlNavigation.require("https://a"), "https://a");
    }

    /** BaseJUnit5Test.open() used to hit a null baseUrl with a bare driver error. */
    @Test
    public void junit5Open_withoutBaseUrl_namesTheMissingKey() {
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setExecution(new SeleniumBootConfig.Execution());
        SeleniumBootContext.initialize(config);
        class Probe extends BaseJUnit5Test { void doOpen() { open(); } void doOpen(String p) { open(p); } }
        assertTrue(expectThrows(IllegalStateException.class, () -> new Probe().doOpen())
                .getMessage().contains("execution.baseUrl"));
        assertTrue(expectThrows(IllegalStateException.class, () -> new Probe().doOpen("/x"))
                .getMessage().contains("execution.baseUrl"));
    }
}
