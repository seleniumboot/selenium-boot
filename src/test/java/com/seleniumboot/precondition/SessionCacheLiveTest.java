package com.seleniumboot.precondition;

import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertNotNull;

/**
 * Live check that a cached session is restored into a browser that is not on the app origin
 * (a fresh driver starts on about:blank, where {@code addCookie} is rejected).
 * Skips when Chrome is not available.
 */
public class SessionCacheLiveTest {

    private HttpServer server;
    private String base;

    @BeforeClass
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            byte[] body = "<html><body>ok</body></html>".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html");
            if (ex.getRequestURI().getPath().equals("/login")) {
                ex.getResponseHeaders().add("Set-Cookie", "sso=abc123; Path=/");
            }
            ex.sendResponseHeaders(200, body.length);
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
    public void restore_fromAboutBlank_restoresCookies() {
        WebDriver driver;
        try {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox");
            driver = new ChromeDriver(o);
        } catch (Throwable t) {
            throw new SkipException("Chrome not available: " + t.getMessage());
        }
        try {
            driver.get(base + "/login");
            SessionCache.store("sso", driver);

            driver.manage().deleteAllCookies();
            driver.get("about:blank");              // where a fresh session sits before the test
            SessionCache.restore("sso", driver);

            driver.get(base + "/");
            assertNotNull(driver.manage().getCookieNamed("sso"),
                    "cookie cached by the provider should be restored for the next test");
        } finally {
            driver.quit();
        }
    }
}
