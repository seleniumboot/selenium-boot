package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.extension.PluginRegistry;
import com.seleniumboot.extension.SeleniumBootPlugin;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link PluginRegistry}.
 */
public class PluginRegistryTest {

    @AfterMethod
    public void resetRegistry() throws Exception {
        Field f = PluginRegistry.class.getDeclaredField("plugins");
        f.setAccessible(true);
        ((List<?>) f.get(null)).clear();
    }

    @Test
    public void register_addsPluginToList() {
        TrackingPlugin plugin = new TrackingPlugin("p1");
        PluginRegistry.register(plugin, minimalConfig());

        assertEquals(PluginRegistry.getPlugins().size(), 1);
        assertEquals(PluginRegistry.getPlugins().get(0).getName(), "p1");
    }

    @Test
    public void register_callsOnLoad() {
        TrackingPlugin plugin = new TrackingPlugin("p2");
        PluginRegistry.register(plugin, minimalConfig());

        assertTrue(plugin.loaded, "onLoad should have been called");
    }

    @Test
    public void unloadAll_callsOnUnloadAndClearsRegistry() {
        TrackingPlugin plugin = new TrackingPlugin("p3");
        PluginRegistry.register(plugin, minimalConfig());
        PluginRegistry.unloadAll();

        assertTrue(plugin.unloaded, "onUnload should have been called");
        assertTrue(PluginRegistry.getPlugins().isEmpty(), "Registry should be empty after unload");
    }

    @Test
    public void unloadAll_pluginUnloadException_doesNotAbort() {
        PluginRegistry.register(new BrokenPlugin(), minimalConfig());
        PluginRegistry.register(new TrackingPlugin("after-broken"), minimalConfig());

        // Should not throw; the second plugin's unload should still run
        PluginRegistry.unloadAll();
        assertTrue(PluginRegistry.getPlugins().isEmpty());
    }

    @Test
    public void getPlugins_returnsUnmodifiableView() {
        PluginRegistry.register(new TrackingPlugin("p4"), minimalConfig());
        assertThrows(UnsupportedOperationException.class,
            () -> PluginRegistry.getPlugins().clear());
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private static SeleniumBootConfig minimalConfig() {
        SeleniumBootConfig config = new SeleniumBootConfig();
        SeleniumBootConfig.Browser browser = new SeleniumBootConfig.Browser();
        browser.setName("chrome");
        config.setBrowser(browser);
        return config;
    }

    static class TrackingPlugin implements SeleniumBootPlugin {
        private final String name;
        boolean loaded;
        boolean unloaded;

        TrackingPlugin(String name) { this.name = name; }

        @Override public String getName() { return name; }
        @Override public void onLoad(SeleniumBootConfig config) { loaded = true; }
        @Override public void onUnload() { unloaded = true; }
    }

    static class BrokenPlugin implements SeleniumBootPlugin {
        @Override public String getName() { return "broken"; }
        @Override public void onUnload() { throw new RuntimeException("simulated unload failure"); }
    }
}
