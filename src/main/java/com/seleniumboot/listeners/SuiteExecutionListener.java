package com.seleniumboot.listeners;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.lifecycle.FrameworkBootstrap;
import com.seleniumboot.metrics.ExecutionMetrics;
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

        } catch (Exception e) {
            // Abort entire suite on bootstrap failure
            throw new IllegalStateException(
                "Selenium Boot failed to initialize. Aborting test suite execution.", e);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        ExecutionMetrics.printSummary();
        DriverManager.quitDriver();
    }
}
