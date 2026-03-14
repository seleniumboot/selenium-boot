package com.seleniumboot.driver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry for custom {@link NamedDriverProvider} implementations.
 *
 * <p>Providers registered here take precedence over the built-in Chrome/Firefox
 * providers but are consulted only for local execution (remote mode always uses
 * {@link RemoteDriverProvider}).
 *
 * <p>SPI providers are loaded automatically via {@link #loadAll()}.
 * Programmatic providers can be added via {@link #register(NamedDriverProvider)}.
 */
public final class DriverProviderRegistry {

    private static final Map<String, DriverProvider> registry = new LinkedHashMap<>();

    private DriverProviderRegistry() {}

    /**
     * Discovers all SPI-registered {@link NamedDriverProvider} implementations
     * and adds them to the registry. Safe to call multiple times.
     */
    public static synchronized void loadAll() {
        ServiceLoader<NamedDriverProvider> loader = ServiceLoader.load(NamedDriverProvider.class);
        for (NamedDriverProvider provider : loader) {
            registry.put(provider.browserName().toLowerCase(), provider);
            System.out.println(
                "[Selenium Boot] Custom DriverProvider registered: " + provider.browserName()
            );
        }
    }

    /**
     * Programmatically registers a custom provider.
     * Overwrites any existing entry for the same browser name.
     */
    public static synchronized void register(NamedDriverProvider provider) {
        registry.put(provider.browserName().toLowerCase(), provider);
    }

    /**
     * Returns the custom provider for the given browser name, or {@code null}
     * if no custom provider is registered for it.
     */
    public static DriverProvider find(String browserName) {
        return registry.get(browserName.toLowerCase());
    }
}
