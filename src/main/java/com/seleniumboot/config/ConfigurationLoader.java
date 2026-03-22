package com.seleniumboot.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public final class ConfigurationLoader {

    private ConfigurationLoader() {
        // utility class
    }

    /**
     * Loads configuration using the following priority chain:
     * <ol>
     *   <li>System property {@code -Dselenium.boot.config=/path/to/file.yml} (explicit override)</li>
     *   <li>{@code ./selenium-boot[-profile].yml} in the current working directory</li>
     *   <li>{@code selenium-boot[-profile].yml} on the classpath (original behaviour)</li>
     * </ol>
     */
    public static SeleniumBootConfig load() {
        String profile = System.getProperty("selenium.boot.profile");

        String configFile = (profile == null || profile.isBlank())
                ? "selenium-boot.yml"
                : "selenium-boot-" + profile + ".yml";

        // Priority 1: explicit path via system property
        String explicitPath = System.getProperty("selenium.boot.config");
        if (explicitPath != null && !explicitPath.isBlank()) {
            return loadFromFile(new File(explicitPath));
        }

        // Priority 2: working directory
        File workingDirFile = new File(configFile);
        if (workingDirFile.exists()) {
            return loadFromFile(workingDirFile);
        }

        // Priority 3: classpath (original behaviour)
        InputStream inputStream = ConfigurationLoader.class
                .getClassLoader()
                .getResourceAsStream(configFile);
        if (inputStream == null) {
            throw new IllegalStateException(
                    "Configuration file '" + configFile + "' not found. " +
                    "Checked: -Dselenium.boot.config, working directory, and classpath.");
        }

        return parseAndValidate(inputStream);
    }

    private static SeleniumBootConfig loadFromFile(File file) {
        if (!file.exists()) {
            throw new IllegalStateException(
                    "Configuration file not found: " + file.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(file)) {
            return parseAndValidate(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read configuration file: " + file.getAbsolutePath(), e);
        }
    }

    private static SeleniumBootConfig parseAndValidate(InputStream inputStream) {
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

        if (config.getBrowser() == null) {
            throw new IllegalStateException("Browser configuration must be specified");
        }
        // browser.name is required unless browser.matrix provides the list of browsers to run
        boolean matrixConfigured = config.getBrowser().getMatrix() != null
                && !config.getBrowser().getMatrix().isEmpty();
        if (!matrixConfigured && config.getBrowser().getName() == null) {
            throw new IllegalStateException(
                    "browser.name must be specified (or use browser.matrix for multi-browser runs)");
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
