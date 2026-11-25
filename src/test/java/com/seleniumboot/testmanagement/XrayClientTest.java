package com.seleniumboot.testmanagement;

import com.seleniumboot.config.SeleniumBootConfig;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;

public class XrayClientTest {

    private HttpServer server;
    private final AtomicReference<String> requestUri = new AtomicReference<>();

    @BeforeMethod
    public void startServer() throws Exception {
        requestUri.set(null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            exchange.getRequestBody().readAllBytes();
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
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
    public void importExecution_stripsTrailingSlashesInServerMode(String suffix) {
        SeleniumBootConfig.TestManagement.Xray config = new SeleniumBootConfig.TestManagement.Xray();
        config.setMode("server");
        config.setJiraUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/jira" + suffix);
        config.setUsername("test-user");
        config.setPassword("test-password");

        new XrayClient(config).importExecution(List.of(new XrayTestResult("QA-1", "PASSED", null)));

        assertEquals(requestUri.get(), "/jira/rest/raven/1.0/import/execution");
    }
}
