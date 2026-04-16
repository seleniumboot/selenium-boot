package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v144.emulation.Emulation;
import org.openqa.selenium.devtools.v144.emulation.model.DevicePosture;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Applies mobile/tablet device emulation to the current browser session.
 *
 * <p>On Chrome/Edge (Chromium), full CDP emulation is used:
 * viewport dimensions, device scale factor, mobile flag, and user-agent are all set.
 *
 * <p>On Firefox and other browsers, a best-effort fallback is applied:
 * the window is resized to the device dimensions and the user-agent is overridden
 * via JavaScript {@code Object.defineProperty}.
 *
 * <pre>
 * // Emulate a built-in profile
 * DeviceEmulator.emulate("iPhone 14");
 *
 * // Emulate a custom profile
 * DeviceProfile custom = new DeviceProfile("My Phone", 390, 844, 3.0, true, "...");
 * DeviceEmulator.emulate(custom);
 *
 * // Reset emulation
 * DeviceEmulator.reset();
 * </pre>
 */
@SeleniumBootApi(since = "1.6.0")
public final class DeviceEmulator {

    private static final Logger LOG = Logger.getLogger(DeviceEmulator.class.getName());

    private DeviceEmulator() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Applies the named device profile (case-insensitive) from {@link DeviceProfiles}.
     *
     * @throws IllegalArgumentException if the name is not found in the registry
     */
    public static void emulate(String deviceName) {
        emulate(DeviceProfiles.get(deviceName));
    }

    /**
     * Applies the given {@link DeviceProfile} to the current driver session.
     */
    public static void emulate(DeviceProfile profile) {
        WebDriver driver = DriverManager.getDriver();
        if (isChromeOrEdge(driver)) {
            applyViaCdp((ChromiumDriver) driver, profile);
        } else {
            applyVisFallback(driver, profile);
        }
        LOG.info("[DeviceEmulator] Emulating: " + profile.getName() +
                 " (" + profile.getWidth() + "x" + profile.getHeight() + ")");
    }

    /**
     * Resets device emulation on Chromium browsers; resizes window back to a
     * standard desktop size on other browsers.
     */
    public static void reset() {
        WebDriver driver = DriverManager.getDriver();
        if (isChromeOrEdge(driver)) {
            resetViaCdp((ChromiumDriver) driver);
        } else {
            driver.manage().window().setSize(new Dimension(1280, 800));
        }
        LOG.info("[DeviceEmulator] Emulation reset.");
    }

    // ------------------------------------------------------------------
    // Chromium CDP path
    // ------------------------------------------------------------------

    private static void applyViaCdp(ChromiumDriver driver, DeviceProfile p) {
        try (DevTools devTools = driver.getDevTools()) {
            devTools.createSession();
            devTools.send(Emulation.setDeviceMetricsOverride(
                    p.getWidth(),
                    p.getHeight(),
                    p.getDeviceScaleFactor(),
                    p.isMobile(),
                    Optional.empty(),       // scale
                    Optional.empty(),       // screenWidth
                    Optional.empty(),       // screenHeight
                    Optional.empty(),       // positionX
                    Optional.empty(),       // positionY
                    Optional.empty(),       // dontSetVisibleSize
                    Optional.empty(),       // screenOrientation
                    Optional.empty(),       // viewport
                    Optional.empty(),       // displayFeature
                    Optional.empty()        // devicePosture
            ));
            devTools.send(Emulation.setUserAgentOverride(
                    p.getUserAgent(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            ));
        }
    }

    private static void resetViaCdp(ChromiumDriver driver) {
        try (DevTools devTools = driver.getDevTools()) {
            devTools.createSession();
            devTools.send(Emulation.clearDeviceMetricsOverride());
            devTools.send(Emulation.setUserAgentOverride(
                    "",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            ));
        }
    }

    // ------------------------------------------------------------------
    // Non-Chromium fallback
    // ------------------------------------------------------------------

    private static void applyVisFallback(WebDriver driver, DeviceProfile p) {
        driver.manage().window().setSize(new Dimension(p.getWidth(), p.getHeight()));
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'userAgent', {" +
                    "  get: function() { return arguments[0]; }," +
                    "  configurable: true" +
                    "});", p.getUserAgent());
        } catch (Exception ignored) {
            LOG.warning("[DeviceEmulator] User-agent override via JS failed (non-Chromium).");
        }
    }

    // ------------------------------------------------------------------

    private static boolean isChromeOrEdge(WebDriver driver) {
        return driver instanceof ChromiumDriver;
    }
}
