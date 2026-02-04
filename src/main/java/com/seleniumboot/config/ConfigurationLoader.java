package com.seleniumboot.config;

import java.io.InputStream;
import java.util.Objects;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class ConfigurationLoader {

    private static final String CONFIG_FILE = "selenium-boot.yml";

    private ConfigurationLoader() {
        // utility class
    }

    public static SeleniumBootConfig load() {
        InputStream inputStream = ConfigurationLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE);

        if (inputStream == null) {
            throw new IllegalStateException(
                    "Configuration file '" + CONFIG_FILE + "' not found in classpath");
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
    }
}
