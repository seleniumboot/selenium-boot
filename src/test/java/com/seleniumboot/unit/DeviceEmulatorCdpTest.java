package com.seleniumboot.unit;

import com.seleniumboot.browser.DeviceEmulator;
import com.seleniumboot.browser.DeviceProfile;
import com.seleniumboot.driver.DriverManager;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Checks the CDP commands {@link DeviceEmulator} sends on Chromium: method names and parameters
 * against the pinned {@code devtools} version, without a browser.
 */
public class DeviceEmulatorCdpTest {

    private boolean pushed;

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        if (pushed) DriverManager.popSessionOverride();
        pushed = false;
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void emulate_onChrome_sendsDeviceMetricsAndUserAgentOverrides() {
        ChromeDriver driver = mock(ChromeDriver.class);
        DevTools devTools = mock(DevTools.class);
        when(driver.getDevTools()).thenReturn(devTools);
        DriverManager.pushSessionOverride(driver);
        pushed = true;

        DeviceEmulator.emulate(new DeviceProfile("Test Phone", 390, 844, 3.0, true, "TestUA/1.0"));

        ArgumentCaptor<Command> sent = ArgumentCaptor.forClass(Command.class);
        verify(devTools).createSession();
        verify(devTools, times(2)).send(sent.capture());
        List<Command> commands = sent.getAllValues();

        Command metrics = commands.get(0);
        assertEquals(metrics.getMethod(), "Emulation.setDeviceMetricsOverride");
        Map<String, Object> p = metrics.getParams();
        assertEquals(p.get("width"), 390);
        assertEquals(p.get("height"), 844);
        assertEquals(((Number) p.get("deviceScaleFactor")).doubleValue(), 3.0);
        assertEquals(p.get("mobile"), true);
        assertFalse(p.containsKey("viewportMeta"), "optional parameters must be omitted, not sent as null");

        Command ua = commands.get(1);
        assertEquals(ua.getMethod(), "Emulation.setUserAgentOverride");
        assertEquals(ua.getParams().get("userAgent"), "TestUA/1.0");
    }
}
