package com.seleniumboot.extension;

import com.seleniumboot.config.SeleniumBootConfig;

/**
 * Extension point for Selenium Boot plugins.
 *
 * <p>Plugins are discovered automatically via Java SPI. Create a file at:
 * <pre>META-INF/services/com.seleniumboot.extension.SeleniumBootPlugin</pre>
 * containing the fully-qualified class name of each implementation.
 *
 * <p>Plugins can also be registered programmatically before framework boot:
 * <pre>PluginRegistry.register(new MyPlugin(), config);</pre>
 */
public interface SeleniumBootPlugin {

    /** Unique human-readable name used in log messages. */
    String getName();

    /**
     * Called once after the framework config is loaded and validated.
     * Use this to read config values and initialise plugin state.
     */
    default void onLoad(SeleniumBootConfig config) {}

    /**
     * Called once when the test suite finishes (after all reports are generated).
     * Use this to flush resources, close connections, etc.
     */
    default void onUnload() {}
}
