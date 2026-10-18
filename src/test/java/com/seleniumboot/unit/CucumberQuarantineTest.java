package com.seleniumboot.unit;

import com.seleniumboot.cucumber.CucumberHooks;
import com.seleniumboot.quarantine.QuarantineLoader;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.opentest4j.TestAbortedException;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectFile;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests verifying quarantine skip behavior for Cucumber scenarios under both
 * TestNG and JUnit Platform.
 */
public class CucumberQuarantineTest {

    private Method checkQuarantineMethod;
    private File tempQuarantineFile;

    @BeforeMethod
    public void setup() throws Exception {
        checkQuarantineMethod = CucumberHooks.class.getDeclaredMethod("checkCucumberQuarantine", Scenario.class);
        checkQuarantineMethod.setAccessible(true);
        QuarantineLoader.reload();
    }

    @AfterMethod
    public void cleanup() {
        Reporter.setCurrentTestResult(null);
        System.clearProperty("selenium.boot.quarantine");
        QuarantineLoader.reload();
        if (tempQuarantineFile != null) {
            tempQuarantineFile.delete();
        }
    }

    // ── TestNG Environment (Reporter.getCurrentTestResult() != null) ───────────

    @Test
    public void checkQuarantine_underTestNg_throwsSkipException_whenTagged() {
        ITestResult mockResult = mock(ITestResult.class);
        Reporter.setCurrentTestResult(mockResult);

        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Quarantined Scenario");
        when(scenario.getSourceTagNames()).thenReturn(List.of("@quarantine"));

        try {
            checkQuarantineMethod.invoke(null, scenario);
            fail("Expected SkipException under TestNG");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SkipException,
                    "Expected SkipException under TestNG, but got: " + e.getCause());
            assertTrue(e.getCause().getMessage().contains("[Quarantined]"));
        }
    }

    @Test
    public void checkQuarantine_underTestNg_throwsSkipException_whenInYaml() throws Exception {
        ITestResult mockResult = mock(ITestResult.class);
        Reporter.setCurrentTestResult(mockResult);

        createQuarantineYaml("quarantine:\n  - login.feature\n");

        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Login with 2FA");
        when(scenario.getUri()).thenReturn(URI.create("classpath:features/login.feature"));
        when(scenario.getSourceTagNames()).thenReturn(Collections.emptyList());

        try {
            checkQuarantineMethod.invoke(null, scenario);
            fail("Expected SkipException under TestNG");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SkipException,
                    "Expected SkipException under TestNG, but got: " + e.getCause());
            assertTrue(e.getCause().getMessage().contains("[Quarantined]"));
        }
    }

    // ── JUnit Platform Environment (Reporter.getCurrentTestResult() == null) ──

    @Test
    public void checkQuarantine_underJUnitPlatform_throwsTestAbortedException_whenTagged() {
        Reporter.setCurrentTestResult(null); // Ensure null to simulate JUnit Platform execution

        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Quarantined Scenario");
        when(scenario.getSourceTagNames()).thenReturn(List.of("@quarantine"));

        try {
            checkQuarantineMethod.invoke(null, scenario);
            fail("Expected TestAbortedException under JUnit Platform");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TestAbortedException,
                    "Expected TestAbortedException under JUnit Platform, but got: " + e.getCause());
            assertTrue(e.getCause().getMessage().contains("[Quarantined]"));
        }
    }

    @Test
    public void checkQuarantine_underJUnitPlatform_throwsTestAbortedException_whenInYaml() throws Exception {
        Reporter.setCurrentTestResult(null);

        createQuarantineYaml("quarantine:\n  - test: checkout.feature\n    reason: \"Payment outage\"\n");

        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Checkout with card");
        when(scenario.getUri()).thenReturn(URI.create("classpath:features/checkout.feature"));
        when(scenario.getSourceTagNames()).thenReturn(Collections.emptyList());

        try {
            checkQuarantineMethod.invoke(null, scenario);
            fail("Expected TestAbortedException under JUnit Platform");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TestAbortedException,
                    "Expected TestAbortedException under JUnit Platform, but got: " + e.getCause());
            assertTrue(e.getCause().getMessage().contains("Payment outage"));
        }
    }

    // ── Non-quarantined scenario ───────────────────────────────────────────────

    @Test
    public void checkQuarantine_notQuarantined_doesNotThrow() throws Exception {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Normal Scenario");
        when(scenario.getSourceTagNames()).thenReturn(List.of("@smoke"));
        when(scenario.getUri()).thenReturn(URI.create("classpath:features/smoke.feature"));

        // Must not throw any exception
        checkQuarantineMethod.invoke(null, scenario);
    }

    // ── End-to-end Cucumber JUnit Platform Engine execution ────────────────────

    public static class QuarantineStepDefinitions {
        @Before("@quarantine")
        public void beforeScenario(Scenario scenario) {
            // In a real run, Cucumber invokes hooks; here we directly invoke quarantine check
            try {
                Method m = CucumberHooks.class.getDeclaredMethod("checkCucumberQuarantine", Scenario.class);
                m.setAccessible(true);
                m.invoke(null, scenario);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException re) throw re;
                throw new RuntimeException(e);
            }
        }

        @Given("a step in quarantined scenario")
        public void a_step() {}
    }

    @Test
    public void cucumberJUnitPlatformEngine_reportsQuarantinedScenarioAsAborted() throws Exception {
        Reporter.setCurrentTestResult(null);

        File tempFeature = File.createTempFile("quarantine_e2e_", ".feature");
        tempFeature.deleteOnExit();
        try (FileWriter w = new FileWriter(tempFeature)) {
            w.write("@quarantine\nFeature: Quarantine E2E\n  Scenario: Quarantined\n    Given a step in quarantined scenario\n");
        }

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectFile(tempFeature))
                .configurationParameter("cucumber.glue", "com.seleniumboot.unit")
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        assertEquals(summary.getTestsAbortedCount(), 1, "Quarantined scenario must be recorded as ABORTED on JUnit Platform");
        assertEquals(summary.getTestsFailedCount(), 0, "Quarantined scenario must NOT be recorded as FAILED on JUnit Platform");
    }

    private void createQuarantineYaml(String content) throws Exception {
        tempQuarantineFile = File.createTempFile("quarantine-test-", ".yml");
        tempQuarantineFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(tempQuarantineFile)) {
            fw.write(content);
        }
        System.setProperty("selenium.boot.quarantine", tempQuarantineFile.getAbsolutePath());
        QuarantineLoader.reload();
    }
}

