package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.testmanagement.TestManagementReporter;
import com.seleniumboot.testmanagement.TestRailCase;
import com.seleniumboot.testmanagement.XrayTest;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.*;

/**
 * Unit tests for TestManagementReporter, TestRailCase, and XrayTest annotation resolution.
 * HTTP clients are not tested here — they require network access.
 */
public class TestManagementReporterTest {

    // ── Annotation resolution helpers ─────────────────────────────────────────

    @TestRailCase("C1234")
    private void methodWithSingleCase() {}

    @TestRailCase({"C1001", "C1002"})
    private void methodWithMultipleCases() {}

    @XrayTest("PROJ-99")
    private void methodWithXrayKey() {}

    @XrayTest({"PROJ-1", "PROJ-2", "PROJ-3"})
    private void methodWithMultipleXrayKeys() {}

    private void methodWithNoAnnotation() {}

    @Test
    public void testRailCaseAnnotation_singleValue() throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("methodWithSingleCase");
        TestRailCase annotation = m.getAnnotation(TestRailCase.class);
        assertNotNull(annotation);
        assertEquals(annotation.value().length, 1);
        assertEquals(annotation.value()[0], "C1234");
    }

    @Test
    public void testRailCaseAnnotation_multipleValues() throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("methodWithMultipleCases");
        TestRailCase annotation = m.getAnnotation(TestRailCase.class);
        assertNotNull(annotation);
        assertEquals(annotation.value().length, 2);
        assertEquals(annotation.value()[0], "C1001");
        assertEquals(annotation.value()[1], "C1002");
    }

    @Test
    public void xrayTestAnnotation_singleKey() throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("methodWithXrayKey");
        XrayTest annotation = m.getAnnotation(XrayTest.class);
        assertNotNull(annotation);
        assertEquals(annotation.value().length, 1);
        assertEquals(annotation.value()[0], "PROJ-99");
    }

    @Test
    public void xrayTestAnnotation_multipleKeys() throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("methodWithMultipleXrayKeys");
        XrayTest annotation = m.getAnnotation(XrayTest.class);
        assertNotNull(annotation);
        assertEquals(annotation.value().length, 3);
    }

    @Test
    public void annotationsAbsent_returnsNull() throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("methodWithNoAnnotation");
        assertNull(m.getAnnotation(TestRailCase.class));
        assertNull(m.getAnnotation(XrayTest.class));
    }

    // ── Reporter: no-op when disabled ────────────────────────────────────────

    @Test
    public void reporter_noOpWhenNotEnabled() throws NoSuchMethodException {
        // With no config loaded, onTestResult should silently do nothing (no exception)
        TestManagementReporter reporter = TestManagementReporter.getInstance();
        Method m = getClass().getDeclaredMethod("methodWithSingleCase");
        // Should not throw even without initialisation
        reporter.onTestResult(m, "PASSED", null);
        reporter.onTestResult(m, "FAILED", "Some failure message");
        reporter.onTestResult(m, "SKIPPED", null);
    }

    @Test
    public void reporter_onSuiteEnd_noOpWhenNotEnabled() {
        // Should not throw when Xray is not initialised
        TestManagementReporter.getInstance().onSuiteEnd();
    }

    // ── Config structure ──────────────────────────────────────────────────────

    @Test
    public void testManagementConfig_defaults() {
        SeleniumBootConfig.TestManagement cfg = new SeleniumBootConfig.TestManagement();

        SeleniumBootConfig.TestManagement.TestRail tr = cfg.getTestrail();
        assertNotNull(tr);
        assertFalse(tr.isEnabled());
        assertEquals(tr.getRunName(), "Selenium Boot Run");
        assertTrue(tr.isAutoCreateRun());
        assertEquals(tr.getRunId(), 0);

        SeleniumBootConfig.TestManagement.Xray xray = cfg.getXray();
        assertNotNull(xray);
        assertFalse(xray.isEnabled());
        assertEquals(xray.getMode(), "cloud");
    }

    @Test
    public void testRailConfig_settersAndGetters() {
        SeleniumBootConfig.TestManagement.TestRail tr = new SeleniumBootConfig.TestManagement.TestRail();
        tr.setEnabled(true);
        tr.setUrl("https://mycompany.testrail.io");
        tr.setUsername("tester@example.com");
        tr.setApiKey("secretKey");
        tr.setProjectId(5);
        tr.setSuiteId(10);
        tr.setRunName("My Run");
        tr.setAutoCreateRun(false);
        tr.setRunId(99);

        assertTrue(tr.isEnabled());
        assertEquals(tr.getUrl(), "https://mycompany.testrail.io");
        assertEquals(tr.getUsername(), "tester@example.com");
        assertEquals(tr.getApiKey(), "secretKey");
        assertEquals(tr.getProjectId(), 5);
        assertEquals(tr.getSuiteId(), 10);
        assertEquals(tr.getRunName(), "My Run");
        assertFalse(tr.isAutoCreateRun());
        assertEquals(tr.getRunId(), 99);
    }

    @Test
    public void xrayConfig_cloudMode_settersAndGetters() {
        SeleniumBootConfig.TestManagement.Xray x = new SeleniumBootConfig.TestManagement.Xray();
        x.setEnabled(true);
        x.setMode("cloud");
        x.setClientId("cid");
        x.setClientSecret("csec");
        x.setProjectKey("PROJ");
        x.setTestPlanKey("PROJ-1");

        assertTrue(x.isEnabled());
        assertEquals(x.getMode(), "cloud");
        assertEquals(x.getClientId(), "cid");
        assertEquals(x.getClientSecret(), "csec");
        assertEquals(x.getProjectKey(), "PROJ");
        assertEquals(x.getTestPlanKey(), "PROJ-1");
    }

    @Test
    public void xrayConfig_serverMode_settersAndGetters() {
        SeleniumBootConfig.TestManagement.Xray x = new SeleniumBootConfig.TestManagement.Xray();
        x.setMode("server");
        x.setJiraUrl("https://jira.example.com");
        x.setUsername("admin");
        x.setPassword("pass123");
        x.setProjectKey("FOO");

        assertEquals(x.getMode(), "server");
        assertEquals(x.getJiraUrl(), "https://jira.example.com");
        assertEquals(x.getUsername(), "admin");
        assertEquals(x.getPassword(), "pass123");
        assertEquals(x.getProjectKey(), "FOO");
    }

    @Test
    public void testManagementConfig_settersAndGetters() {
        SeleniumBootConfig.TestManagement tm = new SeleniumBootConfig.TestManagement();
        SeleniumBootConfig.TestManagement.TestRail tr = new SeleniumBootConfig.TestManagement.TestRail();
        SeleniumBootConfig.TestManagement.Xray xray = new SeleniumBootConfig.TestManagement.Xray();
        tm.setTestrail(tr);
        tm.setXray(xray);
        assertSame(tr, tm.getTestrail());
        assertSame(xray, tm.getXray());
    }

    @Test
    public void seleniumBootConfig_testManagementGetterSetter() {
        SeleniumBootConfig cfg = new SeleniumBootConfig();
        SeleniumBootConfig.TestManagement tm = new SeleniumBootConfig.TestManagement();
        cfg.setTestManagement(tm);
        assertSame(tm, cfg.getTestManagement());
    }
}
