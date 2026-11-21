package com.seleniumboot.assertion;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
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
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Live check of {@code assertThatPage()} on headless Chrome: the page changes its title and
 * URL (client-side, via {@code history.pushState}) 700 ms after load, so a non-retrying
 * assertion would fail. Skips when Chrome is not available.
 */
public class PageAssertLiveTest {

    private HttpServer server;
    private String base;
    private WebDriver driver;

    @BeforeClass
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            byte[] body = ("<html><head><title>Loading</title></head><body>ok<script>"
                    + "setTimeout(function(){document.title='Order 42 | Shop';"
                    + "history.pushState({}, '', '/orders/42');}, 700);"
                    + "</script></body></html>").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();

        if (!SeleniumBootContext.isInitialized()) {
            SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
            timeouts.setExplicit(5);
            SeleniumBootConfig config = new SeleniumBootConfig();
            config.setTimeouts(timeouts);
            SeleniumBootContext.initialize(config);
        }
        try {
            ChromeOptions o = new ChromeOptions();
            o.addArguments("--headless=new", "--no-sandbox");
            driver = new ChromeDriver(o);
        } catch (Throwable t) {
            throw new SkipException("Chrome not available: " + t.getMessage(), t);
        }
        DriverManager.pushSessionOverride(driver);
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        if (driver != null) {
            DriverManager.popSessionOverride();
            driver.quit();
        }
        if (server != null) server.stop(0);
    }

    @Test
    public void waitsForDelayedTitleAndClientSideUrlChange() {
        driver.get(base + "/");

        SeleniumAssert.assertThatPage()
                .hasTitle("Order 42 | Shop")
                .titleContains("Order 42")
                .hasTitle(Pattern.compile("Order \\d+ \\| Shop"))
                .hasUrl(base + "/orders/42")
                .urlContains("/orders/")
                .hasUrl(Pattern.compile(Pattern.quote(base) + "/orders/\\d+"));
    }

    @Test(dependsOnMethods = "waitsForDelayedTitleAndClientSideUrlChange")
    public void failureReportsTheRealTitle() {
        AssertionError e = expectThrows(AssertionError.class,
                () -> SeleniumAssert.assertThatPage().hasTitle("Dashboard"));
        assertTrue(e.getMessage().contains("actual: [Order 42 | Shop]"), e.getMessage());
    }
}
