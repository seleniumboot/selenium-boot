package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig.TestManagement;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class TestManagementClientInterruptTest {

    private static final List<XrayTestResult> RESULTS = List.of(new XrayTestResult("TEST-1", "PASSED", null));

    @Test
    public void testRailRestoresInterruptOnCreateRun() {
        TestManagement.TestRail config = new TestManagement.TestRail();
        config.setUrl("http://127.0.0.1:1");
        TestRailClient client = new TestRailClient(config);

        assertInterruptPreserved(() -> client.createRun(1, 1, "test run"));
    }

    @Test
    public void xrayCloudRestoresInterruptOnAuthentication() {
        TestManagement.Xray config = new TestManagement.Xray();
        config.setMode("cloud");
        XrayClient client = new XrayClient(config);

        assertInterruptPreserved(() -> client.importExecution(RESULTS));
    }

    @Test
    public void xrayServerRestoresInterruptOnImport() {
        TestManagement.Xray config = new TestManagement.Xray();
        config.setMode("server");
        config.setJiraUrl("http://127.0.0.1:1");
        XrayClient client = new XrayClient(config);

        assertInterruptPreserved(() -> client.importExecution(RESULTS));
    }

    private static void assertInterruptPreserved(Runnable request) {
        try {
            Thread.currentThread().interrupt();
            IllegalStateException failure = expectThrows(IllegalStateException.class, request::run);
            assertTrue(failure.getCause() instanceof InterruptedException, "Expected an interrupted HTTP request");
            assertTrue(Thread.currentThread().isInterrupted(), "The caller should still see the interrupt flag");
        } finally {
            Thread.interrupted();
        }
    }
}
