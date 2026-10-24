package com.seleniumboot.network;

import com.seleniumboot.driver.DriverManager;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Live integration test verifying WebDriver BiDi network mocking on real Firefox.
 * Skips gracefully if Firefox or geckodriver is not installed in the environment.
 */
public class FirefoxNetworkMockLiveTest {

    private HttpServer server;
    private int port;
    private WebDriver driver;

    @BeforeClass
    public void setUp() throws Exception {
        // 1. Start lightweight local HTTP server
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = "<!DOCTYPE html><html><body><h1>Firefox BiDi Test</h1></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        port = server.getAddress().getPort();

        // 2. Configure Firefox with BiDi enabled and headless mode
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("-headless");
        options.setCapability("webSocketUrl", true);

        // Check if Firefox is installed via Scoop or standard locations
        String userHome = System.getProperty("user.home");
        File scoopFirefox = new File(userHome, "scoop/apps/firefox/current/firefox.exe");
        if (scoopFirefox.exists()) {
            options.setBinary(scoopFirefox.getAbsolutePath());
        }

        try {
            driver = new FirefoxDriver(options);
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
            DriverManager.pushSessionOverride(driver);
        } catch (Throwable t) {
            if (server != null) {
                server.stop(0);
            }
            throw new SkipException("Firefox or geckodriver not available: " + t.getMessage(), t);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        NetworkMock.cleanup();
        DriverManager.popSessionOverride();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testFirefoxBiDiNetworkMockingReturnJson() {
        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/user/profile")
                .returnJson("{\"name\":\"Alice\",\"browser\":\"firefox\",\"mocked\":true}");

        driver.get("http://127.0.0.1:" + port + "/");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String script =
                "var callback = arguments[arguments.length - 1];\n" +
                "fetch('/api/user/profile')\n" +
                "  .then(response => response.json())\n" +
                "  .then(data => callback(data))\n" +
                "  .catch(err => callback({error: err.toString()}));";

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeAsyncScript(script);

        assertNotNull(result, "Result from fetch should not be null");
        assertEquals(result.get("name"), "Alice");
        assertEquals(result.get("browser"), "firefox");
        assertEquals(result.get("mocked"), Boolean.TRUE);

        mock.clear();
    }

    @Test
    public void testFirefoxBiDiNetworkMockingReturnStatus() {
        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/error")
                .returnStatus(503);

        driver.get("http://127.0.0.1:" + port + "/");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String script =
                "var callback = arguments[arguments.length - 1];\n" +
                "fetch('/api/error')\n" +
                "  .then(response => callback(response.status))\n" +
                "  .catch(err => callback(-1));";

        Object status = js.executeAsyncScript(script);
        assertEquals(status, 503L);

        mock.clear();
    }
}

