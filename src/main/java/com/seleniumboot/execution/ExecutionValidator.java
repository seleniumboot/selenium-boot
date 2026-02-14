package com.seleniumboot.execution;

import com.seleniumboot.config.SeleniumBootConfig;

public class ExecutionValidator {
    private ExecutionValidator() {}

    public static void validate(SeleniumBootConfig.Execution execution) {
        if (execution == null) {
            throw new IllegalStateException("Execution configuration missing");
        }

        String parallel = execution.getParallel();
        if (!"none".equalsIgnoreCase(parallel)
                && !"methods".equalsIgnoreCase(parallel)
                && !"classes".equalsIgnoreCase(parallel)) {
            throw new IllegalStateException("Parallel execution configuration missing");
        }

        int threads = execution.getThreadCount();
        if (threads < 1) {
            throw new IllegalStateException("Thread count must be >= 1");
        }
        int maxAllowed = Runtime.getRuntime().availableProcessors() * 2;
        if (threads > maxAllowed) {
            throw new IllegalStateException(
                    "Thread count " + threads +
                            " exceeds safe limit (" + maxAllowed + ")"
            );
        }
    }
}
