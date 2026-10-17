package com.seleniumboot.junit5;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.ci.BuildThresholdEnforcer;
import com.seleniumboot.ci.GitHubActionsSummary;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.flakiness.FlakinessAnalyzer;
import com.seleniumboot.hooks.HookRegistry;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.lifecycle.FrameworkBootstrap;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.precondition.ApiHealthChecker;
import com.seleniumboot.precondition.PreConditionRegistry;
import com.seleniumboot.reporting.JUnitXmlReporter;
import com.seleniumboot.precondition.PreConditionRunner;
import com.seleniumboot.reporting.ReportAdapterRegistry;
import com.seleniumboot.testmanagement.TestManagementReporter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final AtomicBoolean suiteStarted = new AtomicBoolean(false);

    /**
     * Fires suite-start hooks once per test plan — the JUnit 5 equivalent of
     * {@code SuiteExecutionListener.onStart()}. Bootstrap is idempotent, so this is
     * safe alongside {@link SeleniumBootExtension#beforeAll}. A bootstrap failure is
     * left for the extension to surface with its full cause chain.
     */
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!suiteStarted.compareAndSet(false, true)) return;
        try {
            FrameworkBootstrap.initialize();
            ApiHealthChecker.clearCache();
            PreConditionRegistry.loadAll();
            HookRegistry.onSuiteStart();
            TestManagementReporter.getInstance().onSuiteStart();
        } catch (Exception e) {
            System.err.println("[Selenium Boot] Suite start hooks skipped: " + e.getMessage());
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            ExecutionMetrics.printSummary();
            ExecutionMetrics.exportToJson();
            FlakinessAnalyzer.analyze();

            JUnitXmlReporter.export(ExecutionMetrics.getTimings(), System.currentTimeMillis());

            SeleniumBootConfig config = SeleniumBootContext.getConfig();
            GitHubActionsSummary.export(config, ExecutionMetrics.getTimings());

            ReportAdapterRegistry.generateAll();
            PreConditionRunner.clearAll();
            HookRegistry.onSuiteEnd();
            TestManagementReporter.getInstance().onSuiteEnd();

            if (config != null) {
                BuildThresholdEnforcer.enforce(config, ExecutionMetrics.getTimings());
            }
        } catch (IllegalStateException e) {
            throw e;  // re-throw CI gate failures (pass-rate / flakiness)
        } catch (Exception e) {
            System.err.println("[Selenium Boot] Report generation failed: " + e.getMessage());
        } finally {
            suiteStarted.set(false);
        }
    }
}
