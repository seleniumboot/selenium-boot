package com.seleniumboot.config;

import java.util.List;
import java.util.Map;

public final class SeleniumBootConfig {

    private Browser browser;
    private Execution execution;
    private Retry retry;
    private Timeouts timeouts;

    // --- getters ---
    public Browser getBrowser() {
        return browser;
    }

    public Execution getExecution() {
        return execution;
    }
    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    // --- setters (required for SnakeYAML) ---
    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public Timeouts getTimeouts() {
        return timeouts;
    }
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
    }
    // =========================

    public static final class Browser {
        private String name;
        private boolean headless;
        private List<String> arguments;
        private Map<String, Object> capabilities;

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        public Map<String, Object> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Map<String, Object> capabilities) {
            this.capabilities = capabilities;
        }

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
        private String gridUrl;
        private String parallel = "none";
        private int threadCount = 1;

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

        public void setGridUrl(String gridUrl) {
            this.gridUrl = gridUrl;
        }

        public String getGridUrl() {
            return gridUrl;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public String getParallel() {
            return parallel;
        }

        public void setParallel(String parallel) {
            this.parallel = parallel;
        }
    }

    public static final class Retry {
        private boolean enabled = true;
        private int maxAttempts = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static final class Timeouts {
        private int explicit = 10;
        private int pageLoad = 30;

        public int getExplicit() {
            return explicit;
        }

        public void setExplicit(int explicit) {
            this.explicit = explicit;
        }

        public int getPageLoad() {
            return pageLoad;
        }

        public void setPageLoad(int pageLoad) {
            this.pageLoad = pageLoad;
        }
    }
}
