package com.seleniumboot.wait;

import com.seleniumboot.browser.DeviceEmulator;
import com.seleniumboot.browser.DeviceProfile;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.network.NetworkMock;
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
            boolean html = path.equals("/") || path.equals("/viewport");
            String doc = path.equals("/viewport")
                    ? "<html><head><meta name='viewport' content='width=device-width'></head><body>v</body></html>"
                    : "<html><body>ok</body></html>";
            byte[] body = (html ? doc : "x").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", html ? "text/html" : "text/plain");
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

    @Test
    public void chrome_cdp_waits_doNotDropDeviceEmulation() {
        WebDriver d = requireChrome();
        JavascriptExecutor js = (JavascriptExecutor) d;
        String page = base + "/viewport";
        DriverManager.pushSessionOverride(d);
        try {
            DeviceEmulator.emulate(new DeviceProfile("Live Phone", 390, 844, 3.0, true, "SeleniumBootLiveUA/1.0"));
            d.get(page);
            assertEquals(((Number) js.executeScript("return window.innerWidth;")).intValue(), 390);

            NetworkActivityTracker.awaitIdle(d, Duration.ofMillis(200), Duration.ofSeconds(10));
            js.executeScript("setTimeout(function(){ fetch('/created').catch(function(){}); }, 200);");
            NetworkActivityTracker.awaitResponse(d, "**/created", Duration.ofSeconds(10));
            d.get(page);

            assertEquals(((Number) js.executeScript("return window.innerWidth;")).intValue(), 390,
                    "a network wait must not end the CDP session that holds the emulation (#289)");

            // NetworkMock's CDP fallback must attach to the same session instead of failing silently
            NetworkMock.get().stub("**/stubbed").returnJson("{\"stubbed\":true}");
            Object body = js.executeAsyncScript(
                    "var done = arguments[0]; fetch('/stubbed').then(function(r){ return r.text(); })"
                    + ".then(done, function(e){ done('fetch failed: ' + e); });");
            assertEquals(body, "{\"stubbed\":true}");
            assertEquals(((Number) js.executeScript("return window.innerWidth;")).intValue(), 390);
        } finally {
            NetworkMock.cleanup();
            DeviceEmulator.reset();
            DriverManager.popSessionOverride();
        }
    }

    private WebDriver requireChrome() {
        if (driver == null) throw new SkipException("Chrome not available");
        return driver;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
