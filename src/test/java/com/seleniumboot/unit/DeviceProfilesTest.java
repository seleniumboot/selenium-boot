package com.seleniumboot.unit;

import com.seleniumboot.browser.DeviceProfile;
import com.seleniumboot.browser.DeviceProfiles;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link DeviceProfiles} registry.
 */
public class DeviceProfilesTest {

    @Test
    public void builtIn_iPhone14_exists() {
        Assert.assertTrue(DeviceProfiles.contains("iPhone 14"));
    }

    @Test
    public void builtIn_pixel7_exists() {
        Assert.assertTrue(DeviceProfiles.contains("Pixel 7"));
    }

    @Test
    public void builtIn_iPad_exists() {
        Assert.assertTrue(DeviceProfiles.contains("iPad"));
    }

    @Test
    public void get_caseInsensitive() {
        DeviceProfile p1 = DeviceProfiles.get("iPhone 14");
        DeviceProfile p2 = DeviceProfiles.get("IPHONE 14");
        Assert.assertEquals(p1.getName(), p2.getName());
    }

    @Test
    public void iPhone14_hasMobileFlag() {
        Assert.assertTrue(DeviceProfiles.get("iPhone 14").isMobile());
    }

    @Test
    public void iPad_isNotMobile() {
        Assert.assertFalse(DeviceProfiles.get("iPad").isMobile());
    }

    @Test
    public void iPhone14_dimensions() {
        DeviceProfile p = DeviceProfiles.get("iPhone 14");
        Assert.assertEquals(p.getWidth(), 390);
        Assert.assertEquals(p.getHeight(), 844);
    }

    @Test
    public void pixel7_scaleFactor() {
        DeviceProfile p = DeviceProfiles.get("Pixel 7");
        Assert.assertEquals(p.getDeviceScaleFactor(), 2.625, 0.001);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void get_unknownProfile_throws() {
        DeviceProfiles.get("Unknown Device XYZ");
    }

    @Test
    public void register_customProfile_retrievable() {
        String name = "Test Phone " + System.nanoTime();
        DeviceProfile custom = new DeviceProfile(name, 360, 800, 2.0, true, "TestAgent/1.0");
        DeviceProfiles.register(name, custom);
        DeviceProfile retrieved = DeviceProfiles.get(name);
        Assert.assertEquals(retrieved.getWidth(), 360);
        Assert.assertEquals(retrieved.getUserAgent(), "TestAgent/1.0");
    }

    @Test
    public void names_containsAllBuiltins() {
        Assert.assertTrue(DeviceProfiles.names().contains("iphone 14"));
        Assert.assertTrue(DeviceProfiles.names().contains("pixel 7"));
        Assert.assertTrue(DeviceProfiles.names().contains("ipad"));
        Assert.assertTrue(DeviceProfiles.names().contains("galaxy s23"));
    }
}
