package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;

/**
 * Geolocation mocking helper.
 *
 * <p>On Chrome/Edge: uses CDP {@code Emulation.setGeolocationOverride} for
 * accurate override. On other browsers: injects a JavaScript override of
 * {@code navigator.geolocation.getCurrentPosition} as a fallback.
 *
 * <p>Always call {@link #clear()} or rely on the auto-clear in
 * {@link com.seleniumboot.listeners.TestExecutionListener} after each test.
 *
 * <pre>
 * mockLocation().set(51.5074, -0.1278);  // London
 * open("/map-page");
 * assertThat(By.id("city")).hasText("London");
 * mockLocation().clear();
 * </pre>
 */
@SeleniumBootApi(since = "1.5.0")
public final class GeoLocation {

    private GeoLocation() {
    }

    /** Returns the GeoLocation helper for the current thread's driver. */
    public static GeoLocation instance() {
        return new GeoLocation();
    }

    /**
     * Overrides the browser's geolocation to the given coordinates.
     *
     * @param latitude  decimal degrees (e.g. 51.5074 for London)
     * @param longitude decimal degrees (e.g. -0.1278 for London)
     */
    public void set(double latitude, double longitude) {
        set(latitude, longitude, 0);
    }

    /**
     * Overrides the browser's geolocation including altitude.
     *
     * @param latitude  decimal degrees
     * @param longitude decimal degrees
     * @param altitude  metres above sea level (0 if unknown)
     */
    public void set(double latitude, double longitude, double altitude) {
        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof ChromiumDriver) {
            // Chrome/Edge: CDP override — most accurate
            ((ChromiumDriver) driver).executeCdpCommand(
                    "Emulation.setGeolocationOverride",
                    java.util.Map.of(
                            "latitude",  latitude,
                            "longitude", longitude,
                            "accuracy",  1
                    )
            );
        } else {
            // Firefox / other: JS override
            injectJsOverride(driver, latitude, longitude, altitude);
        }
    }

    /**
     * Removes the geolocation override and restores real location behaviour.
     */
    public void clear() {
        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof ChromiumDriver) {
            ((ChromiumDriver) driver).executeCdpCommand(
                    "Emulation.clearGeolocationOverride",
                    java.util.Map.of()
            );
        } else {
            // Remove JS override
            ((JavascriptExecutor) driver).executeScript(
                    "delete navigator.__defineGetter__; " +
                    "if(window.__seleniumBootGeoBackup) {" +
                    "  Object.defineProperty(navigator, 'geolocation', " +
                    "  { get: function(){ return window.__seleniumBootGeoBackup; }, configurable: true });" +
                    "}"
            );
        }
    }

    // ------------------------------------------------------------------
    // JS fallback for non-Chromium browsers
    // ------------------------------------------------------------------

    private void injectJsOverride(WebDriver driver, double lat, double lon, double alt) {
        String script = String.format(
                "window.__seleniumBootGeoBackup = navigator.geolocation;" +
                "Object.defineProperty(navigator, 'geolocation', {" +
                "  get: function() {" +
                "    return {" +
                "      getCurrentPosition: function(success) {" +
                "        success({" +
                "          coords: { latitude: %f, longitude: %f, altitude: %f," +
                "                    accuracy: 1, altitudeAccuracy: null," +
                "                    heading: null, speed: null }," +
                "          timestamp: Date.now()" +
                "        });" +
                "      }," +
                "      watchPosition: function(success) {" +
                "        success({" +
                "          coords: { latitude: %f, longitude: %f, altitude: %f," +
                "                    accuracy: 1, altitudeAccuracy: null," +
                "                    heading: null, speed: null }," +
                "          timestamp: Date.now()" +
                "        });" +
                "        return 0;" +
                "      }," +
                "      clearWatch: function() {}" +
                "    };" +
                "  }," +
                "  configurable: true" +
                "});",
                lat, lon, alt, lat, lon, alt
        );
        ((JavascriptExecutor) driver).executeScript(script);
    }
}
