package com.seleniumboot.unit;

import com.seleniumboot.driver.DriverProvider;
import com.seleniumboot.driver.DriverProviderRegistry;
import com.seleniumboot.driver.NamedDriverProvider;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link DriverProviderRegistry}.
 */
public class DriverProviderRegistryTest {

    @AfterMethod
    public void resetRegistry() throws Exception {
        Field f = DriverProviderRegistry.class.getDeclaredField("registry");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    @Test
    public void register_findsProviderByExactName() {
        EdgeProvider provider = new EdgeProvider();
        DriverProviderRegistry.register(provider);

        assertNotNull(DriverProviderRegistry.find("edge"));
        assertSame(DriverProviderRegistry.find("edge"), provider);
    }

    @Test
    public void find_isCaseInsensitive() {
        DriverProviderRegistry.register(new EdgeProvider());

        assertNotNull(DriverProviderRegistry.find("Edge"));
        assertNotNull(DriverProviderRegistry.find("EDGE"));
        assertNotNull(DriverProviderRegistry.find("eDgE"));
    }

    @Test
    public void find_unknownBrowser_returnsNull() {
        assertNull(DriverProviderRegistry.find("safari"),
            "Unregistered browser should return null, not throw");
    }

    @Test
    public void register_overwritesExistingEntry() {
        EdgeProvider first = new EdgeProvider();
        EdgeProvider second = new EdgeProvider();
        DriverProviderRegistry.register(first);
        DriverProviderRegistry.register(second);

        assertSame(DriverProviderRegistry.find("edge"), second,
            "Second registration should overwrite the first");
    }

    @Test
    public void register_multipleProviders_eachResolvesByOwnName() {
        SafariProvider safari = new SafariProvider();
        EdgeProvider edge = new EdgeProvider();
        DriverProviderRegistry.register(safari);
        DriverProviderRegistry.register(edge);

        assertSame(DriverProviderRegistry.find("safari"), safari);
        assertSame(DriverProviderRegistry.find("edge"), edge);
    }

    // ----------------------------------------------------------
    // Stub providers (no real driver creation)
    // ----------------------------------------------------------

    static class EdgeProvider implements NamedDriverProvider {
        @Override public String browserName() { return "edge"; }
        @Override public WebDriver createDriver() { return null; }
    }

    static class SafariProvider implements NamedDriverProvider {
        @Override public String browserName() { return "safari"; }
        @Override public WebDriver createDriver() { return null; }
    }
}
