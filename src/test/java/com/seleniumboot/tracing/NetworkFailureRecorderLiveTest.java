package com.seleniumboot.tracing;

import com.seleniumboot.metrics.ExecutionMetrics;
import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.steps.NetworkFailure;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Live check of {@link NetworkFailureRecorder} on real browsers: CDP on Chrome, BiDi on Firefox.
 * Each test skips when its browser or driver is not available.
 */
public class NetworkFailureRecorderLiveTest {

    private HttpServer server;
    private String base;

    @BeforeClass
    public void startServer() throws Exception {
        int closedPort;   // a port nothing listens on and browsers do not block (unlike port 1)
        try (java.net.ServerSocket probe = new java.net.ServerSocket(0)) { closedPort = probe.getLocalPort(); }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            int code = path.equals("/fail") ? 500 : path.equals("/missing") ? 404 : 200;
            byte[] body = path.equals("/")
                    ? ("<html><body>ok<script>"
                       + "fetch('/fail',{method:'POST'}).catch(function(){});"
                       + "fetch('/missing').catch(function(){});"
                       + "fetch('/fine').catch(function(){});"
                       + "fetch('http://127.0.0.1:" + closedPort + "/refused').catch(function(){});"
                       + "</script></body></html>").getBytes(StandardCharsets.UTF_8)
                    : "x".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", path.equals("/") ? "text/html" : "text/plain");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(code, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void chrome_cdp_recordsFailedRequestsOnly() {
        assertFailuresRecorded("live.chrome", () -> {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox");
            return new ChromeDriver(o);
        });
    }

    @Test
    public void firefox_bidi_recordsFailedRequestsOnly() {
        assertFailuresRecorded("live.firefox", () -> {
            FirefoxOptions o = new FirefoxOptions();
            o.addArguments("-headless");
            o.setCapability("webSocketUrl", true);
            return new FirefoxDriver(o);
        });
    }

    @Test
    public void chrome_consoleErrors_arriveWithTimestampsForTheTrace() throws Exception {
        WebDriver driver;
        try {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox");
            driver = new ChromeDriver(o);
        } catch (Throwable t) {
            throw new SkipException("Chrome not available: " + t.getMessage(), t);
        }
        String testId = "live.console";
        try {
            com.seleniumboot.driver.DriverManager.pushSessionOverride(driver);
            com.seleniumboot.internal.SeleniumBootContext.setCurrentTestId(testId);
            ExecutionMetrics.markStart(testId);
            driver.get("data:text/html,<script>console.error('boom from page')</script>");
            sleep(300);
            List<String> messages = com.seleniumboot.browser.ConsoleErrorCollector.collect();

            assertTrue(messages.stream().anyMatch(m -> m.contains("boom from page")), messages.toString());
            TestTiming t = ExecutionMetrics.getTiming(testId);
            assertTrue(t.getConsoleEntries().stream().anyMatch(c -> c.getMessage().contains("boom from page")
                    && c.getOffsetMs() >= 0 && c.getOffsetMs() < 60_000), "entry with a sane offset expected");
        } finally {
            com.seleniumboot.driver.DriverManager.popSessionOverride();
            driver.quit();
        }
    }

    private void assertFailuresRecorded(String testId, Supplier<WebDriver> factory) {
        WebDriver driver;
        try {
            driver = factory.get();
        } catch (Throwable t) {
            throw new SkipException("browser not available: " + t.getMessage(), t);
        }
        try {
            ExecutionMetrics.markStart(testId);
            NetworkFailureRecorder.start(testId, driver);
            driver.get(base + "/");
            waitFor(testId, 3);
            NetworkFailureRecorder.stop(testId);

            TestTiming t = ExecutionMetrics.getTiming(testId);
            List<NetworkFailure> f = t.getNetworkFailures();
            String all = f.stream().map(x -> x.getMethod() + " " + x.getUrl() + " " + x.getStatus() + " " + x.getError()).toList().toString();
            assertTrue(f.stream().anyMatch(x -> x.getUrl().endsWith("/fail") && x.getStatus() == 500 && "POST".equals(x.getMethod())), all);
            assertTrue(f.stream().anyMatch(x -> x.getUrl().endsWith("/missing") && x.getStatus() == 404), all);
            assertTrue(f.stream().anyMatch(x -> x.getUrl().contains("/refused") && x.getStatus() == 0 && x.getError() != null), all);
            assertFalse(f.stream().anyMatch(x -> x.getUrl().endsWith("/fine")), "2xx must not be recorded: " + all);
            assertFalse(f.stream().anyMatch(x -> x.getUrl().endsWith("/favicon.ico") && x.getStatus() < 400), all);

            // stopped: later traffic is not recorded
            int before = f.size();
            driver.get(base + "/fail");
            sleep(500);
            assertEquals(ExecutionMetrics.getTiming(testId).getNetworkFailures().size(), before, "recorder must be stopped");
        } finally {
            NetworkFailureRecorder.stop(testId);
            driver.quit();
        }
    }

    private static void waitFor(String testId, int atLeast) {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            TestTiming t = ExecutionMetrics.getTiming(testId);
            if (t != null && t.getNetworkFailures().size() >= atLeast) return;
            sleep(100);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
