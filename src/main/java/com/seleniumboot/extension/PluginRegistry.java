package com.seleniumboot.extension;

import com.seleniumboot.config.SeleniumBootConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads and manages {@link SeleniumBootPlugin} instances.
 *
 * <p>SPI plugins are discovered automatically; programmatic plugins can be
 * added via {@link #register(SeleniumBootPlugin, SeleniumBootConfig)} before
 * {@link #loadAll(SeleniumBootConfig)} is called.
 */
public final class PluginRegistry {

    private static final List<SeleniumBootPlugin> plugins = new ArrayList<>();

    private PluginRegistry() {}

    /**
     * Discovers all SPI-registered plugins, calls {@code onLoad}, and logs each one.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    public static synchronized void loadAll(SeleniumBootConfig config) {
        if (!plugins.isEmpty()) return;
        ServiceLoader<SeleniumBootPlugin> loader = ServiceLoader.load(SeleniumBootPlugin.class);
        for (SeleniumBootPlugin plugin : loader) {
            plugins.add(plugin);
            plugin.onLoad(config);
            System.out.println("[Selenium Boot] Plugin loaded: " + plugin.getName());
        }
    }

    /**
     * Programmatically registers and immediately activates a plugin.
     * Must be called before framework boot to guarantee correct ordering.
     */
    public static synchronized void register(SeleniumBootPlugin plugin, SeleniumBootConfig config) {
        plugins.add(plugin);
        plugin.onLoad(config);
        System.out.println("[Selenium Boot] Plugin registered: " + plugin.getName());
    }

    /** Calls {@code onUnload} on every plugin and clears the registry. */
    public static synchronized void unloadAll() {
        for (SeleniumBootPlugin plugin : plugins) {
            try {
                plugin.onUnload();
            } catch (Exception e) {
                System.err.println(
                    "[Selenium Boot] Plugin unload error [" + plugin.getName() + "]: " + e.getMessage()
                );
            }
        }
        plugins.clear();
    }

    /** Returns an unmodifiable snapshot of the currently loaded plugins. */
    public static List<SeleniumBootPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }
}
