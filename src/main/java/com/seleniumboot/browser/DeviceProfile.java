package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * Describes a mobile/tablet device for viewport + user-agent emulation.
 *
 * <p>Use the built-in profiles from {@link DeviceProfiles} or create custom ones:
 * <pre>
 * DeviceProfile custom = new DeviceProfile("My Device", 412, 915, 2.625, true,
 *     "Mozilla/5.0 (Linux; Android 13; Pixel 7)...");
 * DeviceProfiles.register("Pixel 7 Custom", custom);
 * </pre>
 */
@SeleniumBootApi(since = "1.6.0")
public final class DeviceProfile {

    private final String name;
    private final int    width;
    private final int    height;
    private final double deviceScaleFactor;
    private final boolean mobile;
    private final String userAgent;

    public DeviceProfile(String name, int width, int height,
                         double deviceScaleFactor, boolean mobile, String userAgent) {
        this.name              = name;
        this.width             = width;
        this.height            = height;
        this.deviceScaleFactor = deviceScaleFactor;
        this.mobile            = mobile;
        this.userAgent         = userAgent;
    }

    public String  getName()              { return name; }
    public int     getWidth()             { return width; }
    public int     getHeight()            { return height; }
    public double  getDeviceScaleFactor() { return deviceScaleFactor; }
    public boolean isMobile()             { return mobile; }
    public String  getUserAgent()         { return userAgent; }
}
