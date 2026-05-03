package com.seleniumboot.junit5;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.ci.BuildThresholdEnforcer;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.flakiness.FlakinessAnalyzer;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.reporting.JUnitXmlReporter;
import com.seleniumboot.precondition.PreConditionRunner;
import com.seleniumboot.reporting.ReportAdapterRegistry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * JUnit Platform launcher listener that generates Selenium Boot reports after
 * the entire test plan finishes — the JUnit 5 equivalent of
 * {@code SuiteExecutionListener.onFinish()} in TestNG.
 *
 * <p>Registered automatically via ServiceLoader:
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}
 *
 * <p>Fires {@code testPlanExecutionFinished} once when all test classes have run,
 * regardless of how many classes were in the plan.
 */
@SeleniumBootApi(since = "1.9.0")
public class SeleniumBootLauncherListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            ExecutionMetrics.printSummary();
            ExecutionMetrics.exportToJson();
            FlakinessAnalyzer.analyze();

            JUnitXmlReporter.export(ExecutionMetrics.getTimings(), System.currentTimeMillis());

            ReportAdapterRegistry.generateAll();
            PreConditionRunner.clearAll();
            HookRegistry.onSuiteEnd();

            SeleniumBootConfig config = SeleniumBootContext.getConfig();
            if (config != null) {
                BuildThresholdEnforcer.enforce(config, ExecutionMetrics.getTimings());
            }
        } catch (IllegalStateException e) {
            throw e;  // re-throw CI gate failures (pass-rate / flakiness)
        } catch (Exception e) {
            System.err.println("[Selenium Boot] Report generation failed: " + e.getMessage());
        }
    }
}
