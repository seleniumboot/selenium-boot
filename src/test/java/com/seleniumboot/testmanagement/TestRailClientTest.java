package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;

public class TestRailClientTest {

    private HttpServer server;
    private final AtomicReference<String> requestUri = new AtomicReference<>();

    @BeforeMethod
    public void startServer() throws Exception {
        requestUri.set(null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            exchange.getRequestBody().readAllBytes();
            byte[] response = "{\"id\":42}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterMethod
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @DataProvider(name = "trailingSlashes")
    public Object[][] trailingSlashes() {
        return new Object[][] {{""}, {"/"}, {"///"}, {"/".repeat(10000)}};
    }

    @Test(dataProvider = "trailingSlashes")
    public void createRun_stripsTrailingSlashes(String suffix) {
        SeleniumBootConfig.TestManagement.TestRail config = new SeleniumBootConfig.TestManagement.TestRail();
        config.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/testrail" + suffix);
        config.setUsername("test-user");
        config.setApiKey("test-key");

        int runId = new TestRailClient(config).createRun(7, 0, "Test run");

        assertEquals(runId, 42);
        assertEquals(requestUri.get(), "/testrail/index.php?/api/v2/add_run/7");
    }
}
