package com.seleniumboot.lifecycle;

import com.seleniumboot.ci.CiEnvironmentDetector;
import com.seleniumboot.config.ConfigurationLoader;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.config.SeleniumBootDefaults;
import com.seleniumboot.driver.DriverProviderRegistry;
import com.seleniumboot.execution.ExecutionValidator;
import com.seleniumboot.extension.PluginRegistry;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.config.SeleniumBootConfig.Notifications;
import com.seleniumboot.reporting.AllureReportAdapter;
import com.seleniumboot.reporting.NotificationAdapter;
import com.seleniumboot.reporting.ReportAdapterRegistry;

/**
 * FrameworkBootstrap is responsible for initializing Selenium Boot
 * before any TestNG execution begins.
 *
 * This class must be invoked exactly once per test suite.
 */
public final class FrameworkBootstrap {

    private FrameworkBootstrap() {
        // utility class
    }

    public static void initialize() {
        if (SeleniumBootContext.isInitialized()) {
            return;
        }
        SeleniumBootConfig config = ConfigurationLoader.load();
        SeleniumBootDefaults.applyMissing(config);
        applyCiOverrides(config);
        ExecutionValidator.validate(config.getExecution());

        SeleniumBootContext.initialize(config);

        // Load all SPI-registered extension points
        DriverProviderRegistry.loadAll();
        HookRegistry.loadAll();
        ReportAdapterRegistry.loadAll();
        PluginRegistry.loadAll(config);

        // Opt-in built-in adapters
        SeleniumBootConfig.Reporting reporting = config.getReporting();
        if (reporting != null && reporting.isAllureEnabled()) {
            ReportAdapterRegistry.register(new AllureReportAdapter());
            System.out.println("[Selenium Boot] Allure adapter enabled → target/allure-results/");
        }

        Notifications notifs = config.getNotifications();
        if (notifs != null) {
            boolean hasSlack = notifs.getSlack() != null
                    && notifs.getSlack().getWebhookUrl() != null
                    && !notifs.getSlack().getWebhookUrl().isBlank();
            boolean hasTeams = notifs.getTeams() != null
                    && notifs.getTeams().getWebhookUrl() != null
                    && !notifs.getTeams().getWebhookUrl().isBlank();
            if (hasSlack || hasTeams) {
                ReportAdapterRegistry.register(new NotificationAdapter(notifs));
                System.out.println("[Selenium Boot] Notification adapter enabled"
                        + (hasSlack ? " [Slack]" : "") + (hasTeams ? " [Teams]" : ""));
            }
        }
    }

    /**
     * When running in CI, auto-apply headless mode and tune thread count
     * to available CPU cores — unless the user has explicitly configured them.
     */
    private static void applyCiOverrides(SeleniumBootConfig config) {
        if (!CiEnvironmentDetector.isCI()) {
            return;
        }

        System.out.println("[Selenium Boot] CI environment detected: "
                + CiEnvironmentDetector.ciName());

        // Force headless — CI agents never have a display
        if (!config.getBrowser().isHeadless()) {
            config.getBrowser().setHeadless(true);
            System.out.println("[Selenium Boot] CI override: browser.headless=true");
        }

        // Auto-tune thread count to CPU cores when the user left the default (1)
        SeleniumBootConfig.Execution execution = config.getExecution();
        if (execution.getThreadCount() == 1 && !"none".equalsIgnoreCase(execution.getParallel())) {
            int recommended = CiEnvironmentDetector.recommendedThreadCount(
                    execution.getMaxActiveSessions());
            if (recommended > 1) {
                execution.setThreadCount(recommended);
                System.out.println("[Selenium Boot] CI override: threadCount=" + recommended
                        + " (derived from available CPU cores)");
            }
        }

        if (CiEnvironmentDetector.isContainer()) {
            System.out.println("[Selenium Boot] Container environment detected — "
                    + "Docker/sandbox flags will be applied to browser options.");
        }
    }
}
