package com.seleniumboot.tracing;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.metrics.ExecutionMetrics;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v153.network.model.LoadingFailed;
import org.openqa.selenium.devtools.v153.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v153.network.model.ResponseReceived;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for failed requests (HTTP 4xx/5xx and network errors) for the length of a test so the
 * trace can list them under the step they happened in. Opt-in via {@code tracing.captureNetwork}.
 *
 * <p>CDP on Chromium, WebDriver BiDi on anything else that supports it; a driver with neither is
 * skipped silently. Never fails a test: every problem is logged and swallowed.
 *
 * <p>Started when a test starts, stopped from {@link ExecutionMetrics#markEnd(String)}, which every
 * runner (TestNG, JUnit 5, Cucumber) calls when a test finishes.
 */
public final class NetworkFailureRecorder {

    private static final Logger LOG = Logger.getLogger(NetworkFailureRecorder.class.getName());

    private static final Map<String, AutoCloseable> ACTIVE = new ConcurrentHashMap<>();

    private NetworkFailureRecorder() {}

    /** Starts listening for the current test if {@code tracing.captureNetwork} (and tracing) is on. */
    public static void startIfEnabled(String testId) {
        try {
            var tracing = SeleniumBootContext.getConfig().getTracing();
            if (tracing == null || !tracing.isEffectivelyEnabled() || !tracing.isCaptureNetwork()) return;
            start(testId, DriverManager.getDriver());
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "[NetworkFailureRecorder] not started: " + e.getMessage());
        }
    }

    static void start(String testId, WebDriver driver) {
        if (testId == null || driver == null) return;
        stop(testId);
        try {
            AutoCloseable listener = null;
            if (driver instanceof ChromiumDriver chromium) {
                listener = listenCdp(testId, chromium);
            } else if (driver instanceof HasBiDi) {
                listener = listenBiDi(testId, driver);
            }
            if (listener != null) ACTIVE.put(testId, listener);
        } catch (Exception | LinkageError e) {
            LOG.warning("[NetworkFailureRecorder] could not start for " + testId + ": " + e.getMessage());
        }
    }

    /** Stops listening for this test; safe to call when nothing was started. */
    public static void stop(String testId) {
        if (testId == null) return;
        AutoCloseable listener = ACTIVE.remove(testId);
        if (listener == null) return;
        try {
            listener.close();
        } catch (Exception e) {
            LOG.log(Level.FINE, "[NetworkFailureRecorder] close failed: " + e.getMessage());
        }
    }

    private static AutoCloseable listenCdp(String testId, ChromiumDriver chromium) {
        DevTools devTools = chromium.getDevTools();
        devTools.createSession();
        devTools.send(org.openqa.selenium.devtools.v153.network.Network.enable(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        Map<String, String[]> seen = new ConcurrentHashMap<>();   // requestId (as text; RequestId has no value equality) -> {method, url}
        devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.requestWillBeSent(),
                (RequestWillBeSent e) -> seen.put(e.getRequestId().toString(),
                        new String[]{e.getRequest().getMethod(), e.getRequest().getUrl()}));
        devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.responseReceived(),
                (ResponseReceived e) -> {
                    int status = e.getResponse().getStatus().intValue();
                    if (status < 400) return;
                    String[] req = seen.get(e.getRequestId().toString());
                    ExecutionMetrics.recordNetworkFailure(testId, req != null ? req[0] : "GET",
                            e.getResponse().getUrl(), status, null);
                });
        devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.loadingFailed(),
                (LoadingFailed e) -> {
                    if (Boolean.TRUE.equals(e.getCanceled().orElse(false))) return;   // aborted by navigation, not a failure
                    String[] req = seen.get(e.getRequestId().toString());
                    ExecutionMetrics.recordNetworkFailure(testId, req != null ? req[0] : "GET",
                            req != null ? req[1] : "(unknown)", 0, e.getErrorText());
                });
        return devTools::close;
    }

    private static AutoCloseable listenBiDi(String testId, WebDriver driver) {
        Network network = new Network(driver);
        network.onResponseCompleted(e -> {
            int status = (int) e.getResponseData().getStatus();
            if (status >= 400) {
                ExecutionMetrics.recordNetworkFailure(testId, e.getRequest().getMethod(),
                        e.getResponseData().getUrl(), status, null);
            }
        });
        network.onFetchError(e -> ExecutionMetrics.recordNetworkFailure(testId, e.getRequest().getMethod(),
                e.getRequest().getUrl(), 0, e.getErrorText()));
        return network;
    }
}
