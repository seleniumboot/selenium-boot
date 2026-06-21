package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.internal.SeleniumBootContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Pushes test results to TestRail and/or Xray after each test method completes,
 * and at suite end (Xray batch import).
 *
 * <p>Wire-up happens automatically via {@code SuiteExecutionListener} and
 * {@code TestExecutionListener} — no user configuration needed beyond
 * {@code testmanagement} in {@code selenium-boot.yml}.
 *
 * <p>Usage in {@code selenium-boot.yml}:
 * <pre>{@code
 * testmanagement:
 *   testrail:
 *     enabled: true
 *     url: https://yourcompany.testrail.io
 *     username: user@example.com
 *     apiKey: YOUR_API_KEY
 *     projectId: 1
 *     suiteId: 2
 *     runName: "Selenium Boot – CI run"
 *
 *   xray:
 *     enabled: true
 *     mode: cloud          # or "server"
 *     clientId: YOUR_CLIENT_ID
 *     clientSecret: YOUR_CLIENT_SECRET
 *     projectKey: PROJ
 * }</pre>
 */
@SeleniumBootApi(since = "3.0.0")
public final class TestManagementReporter {

    private static final Logger LOG = Logger.getLogger(TestManagementReporter.class.getName());
    private static final TestManagementReporter INSTANCE = new TestManagementReporter();

    private TestRailClient testRailClient;
    private XrayClient     xrayClient;

    // Xray collects all results during the run and batch-imports at suite end
    private final List<XrayTestResult> xrayBuffer = new CopyOnWriteArrayList<>();

    private int  activeTestRailRunId  = 0;
    private boolean testRailEnabled   = false;
    private boolean xrayEnabled       = false;

    private TestManagementReporter() {}

    public static TestManagementReporter getInstance() {
        return INSTANCE;
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called by {@code SuiteExecutionListener.onStart()} to initialise clients
     * and optionally create a TestRail run.
     */
    public void onSuiteStart() {
        SeleniumBootConfig.TestManagement cfg = loadConfig();
        if (cfg == null) return;

        SeleniumBootConfig.TestManagement.TestRail trCfg = cfg.getTestrail();
        if (trCfg != null && trCfg.isEnabled() && isSet(trCfg.getUrl()) && isSet(trCfg.getApiKey())) {
            testRailEnabled = true;
            testRailClient  = new TestRailClient(trCfg);
            if (trCfg.isAutoCreateRun() && trCfg.getRunId() == 0) {
                try {
                    activeTestRailRunId = testRailClient.createRun(
                            trCfg.getProjectId(), trCfg.getSuiteId(), trCfg.getRunName());
                    trCfg.setRunId(activeTestRailRunId);
                    LOG.info("[TestRail] Created run id=" + activeTestRailRunId);
                } catch (Exception e) {
                    LOG.warning("[TestRail] Could not create run: " + e.getMessage());
                    testRailEnabled = false;
                }
            } else {
                activeTestRailRunId = trCfg.getRunId();
            }
        }

        SeleniumBootConfig.TestManagement.Xray xCfg = cfg.getXray();
        if (xCfg != null && xCfg.isEnabled()) {
            boolean cloudReady  = "cloud".equalsIgnoreCase(xCfg.getMode())
                    && isSet(xCfg.getClientId()) && isSet(xCfg.getClientSecret());
            boolean serverReady = "server".equalsIgnoreCase(xCfg.getMode())
                    && isSet(xCfg.getJiraUrl()) && isSet(xCfg.getUsername()) && isSet(xCfg.getPassword());
            if (cloudReady || serverReady) {
                xrayEnabled = true;
                xrayClient  = new XrayClient(xCfg);
            }
        }
    }

    /**
     * Called by {@code TestExecutionListener} after each test outcome.
     *
     * @param testMethod the test method whose result was just determined
     * @param status     "PASSED", "FAILED", or "SKIPPED"
     * @param comment    optional detail (e.g. exception message); may be null
     */
    public void onTestResult(Method testMethod, String status, String comment) {
        if (!testRailEnabled && !xrayEnabled) return;

        TestRailCase trAnnotation = resolveAnnotation(testMethod, TestRailCase.class);
        XrayTest     xAnnotation  = resolveAnnotation(testMethod, XrayTest.class);

        if (testRailEnabled && trAnnotation != null) {
            pushToTestRail(trAnnotation.value(), status, comment);
        }

        if (xrayEnabled && xAnnotation != null) {
            for (String key : xAnnotation.value()) {
                xrayBuffer.add(new XrayTestResult(key, status, comment));
            }
        }
    }

    /**
     * Called by {@code SuiteExecutionListener.onFinish()} to flush the Xray batch.
     */
    public void onSuiteEnd() {
        if (xrayEnabled && !xrayBuffer.isEmpty()) {
            try {
                xrayClient.importExecution(new ArrayList<>(xrayBuffer));
                LOG.info("[Xray] Imported " + xrayBuffer.size() + " result(s).");
            } catch (Exception e) {
                LOG.warning("[Xray] Import failed: " + e.getMessage());
            }
            xrayBuffer.clear();
        }
        // Reset state so the reporter can be reused if multiple suites run in the same JVM
        testRailEnabled    = false;
        xrayEnabled        = false;
        activeTestRailRunId = 0;
        testRailClient     = null;
        xrayClient         = null;
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private void pushToTestRail(String[] caseIds, String status, String comment) {
        for (String rawId : caseIds) {
            try {
                int caseId = parseCaseId(rawId);
                testRailClient.addResult(activeTestRailRunId, caseId, status, comment);
                LOG.fine("[TestRail] Pushed " + status + " for case C" + caseId
                         + " in run " + activeTestRailRunId);
            } catch (Exception e) {
                LOG.warning("[TestRail] Could not push result for case " + rawId + ": " + e.getMessage());
            }
        }
    }

    /** Strips leading "C" and parses the numeric case ID. */
    private int parseCaseId(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("C") || trimmed.startsWith("c")) trimmed = trimmed.substring(1);
        return Integer.parseInt(trimmed);
    }

    /** Looks for the annotation first on the method, then on the declaring class. */
    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(Method method, Class<A> type) {
        A a = method.getAnnotation(type);
        if (a != null) return a;
        return method.getDeclaringClass().getAnnotation(type);
    }

    private SeleniumBootConfig.TestManagement loadConfig() {
        try {
            SeleniumBootConfig config = SeleniumBootContext.getConfig();
            if (config == null) return null;
            return config.getTestManagement();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
