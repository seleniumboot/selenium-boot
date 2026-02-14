package com.seleniumboot.driver;

import java.util.List;

public class BrowserArgumentValidator {
    private  BrowserArgumentValidator() {
    }

    public static void validate(String browser, List<String> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return;
        }

        if ("firefox".equalsIgnoreCase(browser)) {
            validateFirefox(arguments);
        }

        if ("chrome".equalsIgnoreCase(browser)) {
            validateChrome(arguments);
        }
    }

    private static void validateFirefox( List<String> arguments) {
        for (String argument : arguments) {
            if ((argument.startsWith("--remote-allow-origins"))) {
                throw new IllegalStateException(
                        "Invalid argument'" + argument + "' for Firefox."
                );
            }
        }
    }

    private static void validateChrome(List<String> arguments) {
        for (String argument : arguments) {
            if ((argument.startsWith("-private"))) {
                throw new IllegalStateException(
                        "Invalid argument'" + argument + "' for Chrome."
                );
            }
        }
    }

}
