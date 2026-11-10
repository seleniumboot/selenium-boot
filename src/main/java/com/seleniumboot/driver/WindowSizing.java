package com.seleniumboot.driver;

import java.util.List;

/**
 * Decides whether a locally launched, visible browser should start maximized.
 *
 * <p>Maximizing is the default for headed local runs, so a watched test is not left in a
 * small default window. It is skipped when the window size is already decided elsewhere:
 * headless runs and containers (explicit 1920x1080), or the user's own browser arguments.
 */
final class WindowSizing {

    private WindowSizing() {}

    static boolean shouldMaximize(boolean headless, boolean container, List<String> userArguments) {
        if (headless || container) {
            return false;
        }
        if (userArguments != null) {
            for (String arg : userArguments) {
                String a = arg == null ? "" : arg.toLowerCase();
                if (a.startsWith("--start-maximized") || a.startsWith("--start-fullscreen")
                        || a.startsWith("--kiosk") || a.startsWith("-kiosk")
                        || a.startsWith("--window-size") || a.startsWith("--width")
                        || a.startsWith("--height") || a.startsWith("-width")
                        || a.startsWith("-height")) {
                    return false;
                }
            }
        }
        return true;
    }
}
