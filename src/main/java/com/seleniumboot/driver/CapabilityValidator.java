package com.seleniumboot.driver;

import java.util.Map;

public class CapabilityValidator {
    private CapabilityValidator(){}

    public static void validate(String browser, Map<String,Object> capabilities){
        if (capabilities ==null || capabilities.isEmpty()) {
            return;
        }
        for (String key : capabilities.keySet()) {
            validateNameSpace(browser, key);
        }
    }

    private static void validateNameSpace(String browser, String key){
//        block chrome namespaces in firefox
        if ("firefox".equalsIgnoreCase(browser) && key.startsWith("goog:")) {
            throw new IllegalStateException(
                    "Capability '" + key + "' is not valid for Firefox"
            );
        }

//        block Firefox namespace in chrome
        if ("chrome".equalsIgnoreCase(browser) && key.startsWith("moz:")) {
            throw new IllegalStateException(
                    "Capability '" + key + "' is not valid for Chrome"
            );
        }
    }
}
