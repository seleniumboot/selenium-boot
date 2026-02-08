package com.seleniumboot.config;

public final class SeleniumBootConfig {

    private Browser browser;
    private Execution execution;

    // --- getters ---
    public Browser getBrowser() {
        return browser;
    }

    public Execution getExecution() {
        return execution;
    }

    // --- setters (required for SnakeYAML) ---
    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    // =========================

    public static final class Browser {
        private String name;
        private boolean headless;

        public String getName() {
            return name;
        }

        public boolean isHeadless() {
            return headless;
        }

        // setters REQUIRED
        public void setName(String name) {
            this.name = name;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
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

        // setters REQUIRED
        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
