package com.seleniumboot.listeners;

import com.seleniumboot.ci.BuildThresholdEnforcer;
import com.seleniumboot.flakiness.FlakinessAnalyzer;
import com.seleniumboot.healing.HealLog;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.lifecycle.FrameworkBootstrap;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.extension.PluginRegistry;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.precondition.ApiHealthChecker;
import com.seleniumboot.precondition.PreConditionRegistry;
import com.seleniumboot.precondition.PreConditionRunner;
import com.seleniumboot.reporting.JUnitXmlReporter;
import com.seleniumboot.reporting.ReportAdapterRegistry;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.xml.XmlSuite;

/**
 * SuiteExecutionListener is responsible for framework initialization
 * before any tests are executed.
 *
 * This listener must run exactly once per TestNG suite.
 */
public final class SuiteExecutionListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        try {
//            Initialize framework(loads + validate configs)
            FrameworkBootstrap.initialize();

//            Fetch configs
            SeleniumBootConfig config = SeleniumBootContext.getConfig();
            SeleniumBootConfig.Execution execution = config.getExecution();

//            Apply Parallel config
            if (!"none".equalsIgnoreCase(execution.getParallel())) {
                XmlSuite xmlSuite = suite.getXmlSuite();

                XmlSuite.ParallelMode mode =
                        XmlSuite.ParallelMode.valueOf(
                                execution.getParallel().toUpperCase()
                        );

                xmlSuite.setParallel(mode);
                xmlSuite.setThreadCount(
                        execution.getThreadCount()
                );

                System.out.println(
                        "[Selenium Boot] Parallel mode: "
                                + mode + " | Threads: "
                                + execution.getThreadCount()
                );
            } else {
                System.out.println(
                        "[Selenium Boot] Parallel execution disabled."
                );
            }

            ApiHealthChecker.clearCache(); // reset per-suite health check cache
            PreConditionRegistry.loadAll();
            HookRegistry.onSuiteStart();

        } catch (Exception e) {
            // Abort entire suite on bootstrap failure
            throw new IllegalStateException(
                "Selenium Boot failed to initialize. Aborting test suite execution.", e);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        ExecutionMetrics.printSummary();
        ExecutionMetrics.exportToJson();
        HealLog.export();
        FlakinessAnalyzer.analyze();

        // Machine-readable JUnit XML for CI test result parsing
        JUnitXmlReporter.export(
                ExecutionMetrics.getTimings(),
                suite.getAllInvokedMethods().size() > 0
                        ? System.currentTimeMillis() : 0L);

        ReportAdapterRegistry.generateAll();
        PreConditionRunner.clearAll();
        DriverManager.quitAllSuiteDrivers(); // per-suite lifecycle — quits all kept-alive drivers
        DriverManager.quitDriver();          // per-test safety net — no-op if already quit
        HookRegistry.onSuiteEnd();
        PluginRegistry.unloadAll();

        // Build quality gates — must run last so all metrics are recorded
        SeleniumBootConfig config = SeleniumBootContext.getConfig();
        BuildThresholdEnforcer.enforce(config, ExecutionMetrics.getTimings());
    }
}
