package com.seleniumboot.wait;

import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Live check of the CDP path of {@link NetworkActivityTracker} on a real Chrome.
 * Skips when Chrome or its driver is not available.
 */
public class NetworkActivityTrackerLiveTest {

    private HttpServer server;
    private String base;
    private WebDriver driver;

    @BeforeClass
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/slow")) sleep(400);
            int code = path.equals("/created") ? 201 : 200;
            byte[] body = (path.equals("/") ? "<html><body>ok</body></html>" : "x").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", path.equals("/") ? "text/html" : "text/plain");
            ex.sendResponseHeaders(code, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox");
            driver = new ChromeDriver(o);
        } catch (Throwable t) {
            driver = null;
        }
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        if (driver != null) driver.quit();
        if (server != null) server.stop(0);
    }

    @Test
    public void chrome_cdp_awaitResponse_returnsStatusOfMatchingResponse() {
        WebDriver d = requireChrome();
        d.get(base + "/");
        ((JavascriptExecutor) d).executeScript(
                "setTimeout(function(){ fetch('/created').catch(function(){}); }, 300);");

        int status = NetworkActivityTracker.awaitResponse(d, "**/created", Duration.ofSeconds(10));

        assertEquals(status, 201);
    }

    @Test
    public void chrome_cdp_awaitIdle_waitsForInFlightRequest() {
        WebDriver d = requireChrome();
        d.get(base + "/");
        ((JavascriptExecutor) d).executeScript(
                "window.__done = false; setTimeout(function(){ fetch('/slow').then(function(){ window.__done = true; }); }, 100);");

        NetworkActivityTracker.awaitIdle(d, Duration.ofMillis(500), Duration.ofSeconds(10));

        assertTrue((Boolean) ((JavascriptExecutor) d).executeScript("return window.__done;"),
                "idle must not be reported while /slow is still in flight");
    }

    private WebDriver requireChrome() {
        if (driver == null) throw new SkipException("Chrome not available");
        return driver;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
