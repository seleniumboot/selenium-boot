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

    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_MODE = "local";
    private static final int DEFAULT_EXPLICIT_TIMEOUT = 10;
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;

    private ConfigurationLoader() {
        // utility class
    }

    /**
     * Loads configuration using the following priority chain:
     * <ol>
     *   <li>System property {@code -Dselenium.boot.config=/path/to/file.yml} (explicit override)</li>
     *   <li>{@code ./selenium-boot[-profile].yml} in the current working directory</li>
     *   <li>{@code selenium-boot[-profile].yml} on the classpath (original behaviour)</li>
     *   <li>Built-in defaults — only when <em>no profile and no explicit path</em> was requested
     *       and the default {@code selenium-boot.yml} exists nowhere</li>
     * </ol>
     *
     * <p>Fields a file leaves out fall back to the built-in defaults ({@link #applyBuiltInDefaults});
     * values that are present but invalid still fail validation. A requested profile or explicit
     * path that cannot be found is always an error — silently falling back would hide a typo.
     */
    public static SeleniumBootConfig load() {
        return load(new File("."), ConfigurationLoader.class.getClassLoader());
    }

    /** Same as {@link #load()} with the working directory and classpath supplied — for tests. */
    public static SeleniumBootConfig load(File workingDir, ClassLoader classLoader) {
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
        File workingDirFile = new File(workingDir, configFile);
        if (workingDirFile.exists()) {
            return loadFromFile(workingDirFile);
        }

        // Priority 3: classpath (original behaviour)
        InputStream inputStream = classLoader.getResourceAsStream(configFile);
        if (inputStream == null) {
            if (profile == null || profile.isBlank()) {
                System.out.println("[Selenium Boot] No " + configFile + " found — running with built-in defaults ("
                        + DEFAULT_BROWSER + ", " + DEFAULT_MODE + ", timeouts.explicit=" + DEFAULT_EXPLICIT_TIMEOUT
                        + "s, timeouts.pageLoad=" + DEFAULT_PAGE_LOAD_TIMEOUT
                        + "s). Add a " + configFile + " to customise.");
                SeleniumBootConfig config = new SeleniumBootConfig();
                completeAndValidate(config);
                return config;
            }
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
        if (config == null) {
            config = new SeleniumBootConfig(); // empty file — every field falls back to defaults
        }

        completeAndValidate(config);

        return config;
    }

    private static void completeAndValidate(SeleniumBootConfig config) {
        // Sections a file omits entirely are created so their fields can be defaulted.
        if (config.getBrowser() == null) config.setBrowser(new SeleniumBootConfig.Browser());
        if (config.getExecution() == null) config.setExecution(new SeleniumBootConfig.Execution());
        if (config.getTimeouts() == null) config.setTimeouts(new SeleniumBootConfig.Timeouts());

        // Programmatic defaults (SeleniumBootDefaults) take precedence over the built-in ones.
        SeleniumBootDefaults.applyMissing(config);
        applyBuiltInDefaults(config);

        validate(config);
    }

    /**
     * Fills fields the file left out. Only <em>absent</em> values are defaulted:
     * {@code browser.name} (unless {@code browser.matrix} is set), {@code execution.mode},
     * and zero-valued timeouts. A negative timeout or an unrecognised mode is still rejected by
     * {@link #validate}.
     */
    private static void applyBuiltInDefaults(SeleniumBootConfig config) {
        SeleniumBootConfig.Browser browser = config.getBrowser();
        boolean matrixConfigured = browser.getMatrix() != null && !browser.getMatrix().isEmpty();
        if (browser.getName() == null && !matrixConfigured) {
            browser.setName(DEFAULT_BROWSER);
        }
        if (config.getExecution().getMode() == null) {
            config.getExecution().setMode(DEFAULT_MODE);
        }
        if (config.getTimeouts().getExplicit() == 0) {
            config.getTimeouts().setExplicit(DEFAULT_EXPLICIT_TIMEOUT);
        }
        if (config.getTimeouts().getPageLoad() == 0) {
            config.getTimeouts().setPageLoad(DEFAULT_PAGE_LOAD_TIMEOUT);
        }
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
