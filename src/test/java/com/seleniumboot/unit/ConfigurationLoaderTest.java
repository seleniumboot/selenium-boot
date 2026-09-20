package com.seleniumboot.unit;

import com.seleniumboot.config.ConfigurationLoader;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.config.SeleniumBootDefaults;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ConfigurationLoader}.
 * Relies on selenium-boot.yml present in src/test/resources (classpath).
 */
public class ConfigurationLoaderTest {

    @BeforeMethod
    public void clearSystemProperties() {
        System.clearProperty("selenium.boot.config");
        System.clearProperty("selenium.boot.profile");
    }

    @AfterMethod
    public void restoreSystemProperties() {
        System.clearProperty("selenium.boot.config");
        System.clearProperty("selenium.boot.profile");
    }

    // ----------------------------------------------------------
    // Classpath loading (Priority 3)
    // ----------------------------------------------------------

    @Test
    public void load_defaultProfile_loadsFromClasspath() {
        SeleniumBootConfig config = ConfigurationLoader.load();

        assertNotNull(config);
        assertNotNull(config.getBrowser());
        assertEquals("chrome", config.getBrowser().getName());
        assertEquals("local", config.getExecution().getMode());
        assertTrue(config.getTimeouts().getExplicit() > 0);
        assertTrue(config.getTimeouts().getPageLoad() > 0);
    }

    @Test
    public void load_returnsRetryConfig() {
        SeleniumBootConfig config = ConfigurationLoader.load();
        assertNotNull(config.getRetry());
    }

    @Test
    public void load_returnsExecutionBaseUrl() {
        SeleniumBootConfig config = ConfigurationLoader.load();
        assertNotNull(config.getExecution().getBaseUrl());
        assertFalse(config.getExecution().getBaseUrl().isBlank());
    }

    // ----------------------------------------------------------
    // Profile selection
    // ----------------------------------------------------------

    @Test
    public void load_withProfile_loadsProfileFile() {
        System.setProperty("selenium.boot.profile", "prod");
        SeleniumBootConfig config = ConfigurationLoader.load();
        assertNotNull(config);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void load_withNonExistentProfile_throwsIllegalState() {
        System.setProperty("selenium.boot.profile", "does-not-exist-xyz");
        ConfigurationLoader.load(); // should throw
    }

    // ----------------------------------------------------------
    // Explicit path (Priority 1)
    // ----------------------------------------------------------

    @Test(expectedExceptions = IllegalStateException.class)
    public void load_withExplicitPathThatDoesNotExist_throwsIllegalState() {
        System.setProperty("selenium.boot.config", "/nonexistent/path/config.yml");
        ConfigurationLoader.load();
    }

    // ----------------------------------------------------------
    // Validation
    // ----------------------------------------------------------

    @Test
    public void load_configHasPositiveThreadCount() {
        SeleniumBootConfig config = ConfigurationLoader.load();
        assertTrue(config.getExecution().getMaxActiveSessions() > 0);
    }

    // ----------------------------------------------------------
    // Zero-config first run: defaults for what a file leaves out
    // ----------------------------------------------------------

    /** A classpath with no selenium-boot*.yml on it (null parent = no test resources). */
    private static ClassLoader emptyClasspath() {
        return new URLClassLoader(new URL[0], null);
    }

    private static File emptyWorkingDir() throws IOException {
        return Files.createTempDirectory("sb-config").toFile();
    }

    @Test
    public void load_noConfigFileAnywhere_usesBuiltInDefaults() throws IOException {
        SeleniumBootConfig config = ConfigurationLoader.load(emptyWorkingDir(), emptyClasspath());

        assertEquals(config.getBrowser().getName(), "chrome");
        assertEquals(config.getExecution().getMode(), "local");
        assertEquals(config.getTimeouts().getExplicit(), 10);
        assertEquals(config.getTimeouts().getPageLoad(), 30);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void load_noFileButProfileRequested_stillThrows() throws IOException {
        System.setProperty("selenium.boot.profile", "staging");
        ConfigurationLoader.load(emptyWorkingDir(), emptyClasspath());
    }

    @Test
    public void load_emptyFile_usesBuiltInDefaults() throws IOException {
        Path file = Files.createTempFile("sb-empty", ".yml");
        System.setProperty("selenium.boot.config", file.toString());

        SeleniumBootConfig config = ConfigurationLoader.load();

        assertEquals(config.getBrowser().getName(), "chrome");
        assertEquals(config.getTimeouts().getExplicit(), 10);
    }

    @Test
    public void load_partialFile_keepsGivenValuesAndDefaultsTheRest() throws IOException {
        Path file = Files.createTempFile("sb-partial", ".yml");
        Files.writeString(file, "browser:\n  name: firefox\n  headless: true\ntimeouts:\n  explicit: 5\n");
        System.setProperty("selenium.boot.config", file.toString());

        SeleniumBootConfig config = ConfigurationLoader.load();

        assertEquals(config.getBrowser().getName(), "firefox");
        assertTrue(config.getBrowser().isHeadless());
        assertEquals(config.getTimeouts().getExplicit(), 5);
        assertEquals(config.getTimeouts().getPageLoad(), 30, "omitted pageLoad falls back to the default");
        assertEquals(config.getExecution().getMode(), "local", "omitted execution section falls back");
    }

    @Test
    public void load_matrixWithoutName_doesNotInjectDefaultBrowserName() throws IOException {
        Path file = Files.createTempFile("sb-matrix", ".yml");
        Files.writeString(file, "browser:\n  matrix: [chrome, firefox]\n");
        System.setProperty("selenium.boot.config", file.toString());

        SeleniumBootConfig config = ConfigurationLoader.load();

        assertNull(config.getBrowser().getName());
        assertEquals(config.getBrowser().getMatrix().size(), 2);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void load_invalidModeStillRejected() throws IOException {
        Path file = Files.createTempFile("sb-badmode", ".yml");
        Files.writeString(file, "execution:\n  mode: banana\n");
        System.setProperty("selenium.boot.config", file.toString());
        ConfigurationLoader.load();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void load_negativeTimeoutStillRejected() throws IOException {
        Path file = Files.createTempFile("sb-negtimeout", ".yml");
        Files.writeString(file, "timeouts:\n  explicit: -3\n");
        System.setProperty("selenium.boot.config", file.toString());
        ConfigurationLoader.load();
    }
}
