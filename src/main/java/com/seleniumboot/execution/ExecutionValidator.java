package com.seleniumboot.execution;

import com.seleniumboot.config.SeleniumBootConfig;
import org.testng.xml.XmlSuite;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ExecutionValidator {
    private ExecutionValidator() {}

    public static void validate(SeleniumBootConfig.Execution execution) {
        if (execution == null) {
            throw new IllegalStateException("Execution configuration missing");
        }

        String parallel = execution.getParallel();
        if (parallel == null || parallel.trim().isEmpty()) {
            throw new IllegalStateException("Parallel execution configuration missing");
        }
        try {
            // Delegate to TestNG's own enum rather than a hand-written allowlist — the
            // value is passed straight to XmlSuite.setParallel() downstream, so anything
            // TestNG accepts, Selenium Boot accepts.
            XmlSuite.ParallelMode.valueOf(parallel.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Invalid execution.parallel value '" + parallel +
                            "' — valid values are " + validParallelModes()
            );
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

    private static String validParallelModes() {
        return Arrays.stream(XmlSuite.ParallelMode.values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
