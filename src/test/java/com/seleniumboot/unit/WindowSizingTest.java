package com.seleniumboot.unit;

import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class WindowSizingTest {

    private static boolean maximize(boolean headless, boolean container, List<String> args) throws Exception {
        Class<?> c = Class.forName("com.seleniumboot.driver.WindowSizing");
        Method m = c.getDeclaredMethod("shouldMaximize", boolean.class, boolean.class, List.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, headless, container, args);
    }

    @Test public void headedLocalMaximizesByDefault() throws Exception {
        assertEquals(maximize(false, false, null), true);
        assertEquals(maximize(false, false, List.of("--disable-notifications")), true);
    }

    @Test public void headlessAndContainerDoNot() throws Exception {
        assertEquals(maximize(true, false, null), false);
        assertEquals(maximize(false, true, null), false);
    }

    @Test public void userSizingArgumentsWin() throws Exception {
        for (String a : List.of("--start-maximized", "--window-size=800,600", "--kiosk", "--width=900", "-height=700")) {
            assertEquals(maximize(false, false, List.of(a)), false, a);
        }
    }
}
