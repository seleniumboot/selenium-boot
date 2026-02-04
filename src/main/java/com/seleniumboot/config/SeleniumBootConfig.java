package com.seleniumboot.config;

public final class SeleniumBootConfig {

    private Browser browser;
    private Execution execution;

    public Browser getBrowser() {
        return browser;
    }

    public Execution getExecution() {
        return execution;
    }

    public static final class Browser {
        private String name;
        private boolean headless;

        public String getName() {
            return name;
        }

        public boolean isHeadless() {
            return headless;
        }
    }

    public static final class Execution {
        private String mode;
        private String baseUrl;

        public String getMode() {
            return mode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }
    }
}
