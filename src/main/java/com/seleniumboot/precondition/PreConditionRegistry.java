package com.seleniumboot.precondition;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Registry for {@link BaseConditions} provider classes.
 *
 * <p>Providers are discovered via Java SPI or registered programmatically:
 *
 * <pre>
 * // SPI — create:
 * // META-INF/services/com.seleniumboot.precondition.BaseConditions
 * // containing: com.example.AppConditions
 *
 * // Programmatic
 * PreConditionRegistry.register(new AppConditions());
 * </pre>
 */
@SeleniumBootApi(since = "0.8.0")
public final class PreConditionRegistry {

    private static final List<BaseConditions> providers = new ArrayList<>();
    private static volatile boolean loaded = false;

    private PreConditionRegistry() {}

    /**
     * Discovers all SPI-registered {@link BaseConditions} providers.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    public static synchronized void loadAll() {
        if (loaded) return;
        ServiceLoader<BaseConditions> loader = ServiceLoader.load(BaseConditions.class);
        for (BaseConditions provider : loader) {
            providers.add(provider);
            System.out.println("[Selenium Boot] PreCondition provider loaded: " + provider.getClass().getSimpleName());
        }
        loaded = true;
    }

    /**
     * Programmatically registers a provider.
     * Must be called before the suite starts.
     */
    public static synchronized void register(BaseConditions provider) {
        providers.add(provider);
        System.out.println("[Selenium Boot] PreCondition provider registered: " + provider.getClass().getSimpleName());
    }

    /**
     * Finds the provider method for the given condition name.
     *
     * @param conditionName the value of {@link ConditionProvider#value()}
     * @return a resolved {@link ProviderMethod}, or {@code null} if not found
     */
    static ProviderMethod find(String conditionName) {
        for (BaseConditions provider : providers) {
            for (Method method : provider.getClass().getMethods()) {
                ConditionProvider annotation = method.getAnnotation(ConditionProvider.class);
                if (annotation != null && annotation.value().equals(conditionName)) {
                    return new ProviderMethod(provider, method);
                }
            }
        }
        return null;
    }

    /** Returns an unmodifiable view of all registered providers. */
    public static List<BaseConditions> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    /** Package-private — pairs a provider instance with its resolved method. */
    static final class ProviderMethod {
        final BaseConditions instance;
        final Method method;

        ProviderMethod(BaseConditions instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        void invoke() throws Exception {
            method.invoke(instance);
        }
    }
}
