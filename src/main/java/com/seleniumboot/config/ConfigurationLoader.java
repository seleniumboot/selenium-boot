package com.seleniumboot.config;

import java.io.InputStream;
import java.util.Objects;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class ConfigurationLoader {

    private ConfigurationLoader() {
        // utility class
    }

    public static SeleniumBootConfig load() {
        String profile = System.getProperty("selenium.boot.profile");

        String configFile = (profile == null || profile.isBlank())
                ? "selenium-boot.yml"
                : "selenium-boot-" + profile + ".yml";

        InputStream inputStream = ConfigurationLoader.class
                .getClassLoader()
                .getResourceAsStream(configFile);
        if (inputStream == null) {
            throw new IllegalStateException(
                    "Configuration file '" + configFile + "' not found in classpath");
        }

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor =
                new Constructor(SeleniumBootConfig.class, loaderOptions);

        Yaml yaml = new Yaml(constructor);
        SeleniumBootConfig config = yaml.load(inputStream);

        validate(config);

        return config;
    }

    private static void validate(SeleniumBootConfig config) {
        Objects.requireNonNull(config, "Configuration must not be null");

        if (config.getBrowser() == null || config.getBrowser().getName() == null) {
            throw new IllegalStateException("Browser name must be specified");
        }

        if (config.getExecution() == null || config.getExecution().getMode() == null) {
            throw new IllegalStateException("Execution mode must be specified");
        }

        String mode = config.getExecution().getMode();
        if (!"local".equalsIgnoreCase(mode) && !"remote".equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    "execution.mode must be 'local' or 'remote', got: '" + mode + "'");
        }

        if (config.getTimeouts() == null
                || config.getTimeouts().getExplicit() <= 0
                || config.getTimeouts().getPageLoad() <= 0) {
            throw new IllegalStateException(
                    "timeouts.explicit and timeouts.pageLoad must be configured with positive values");
        }
    }
}
