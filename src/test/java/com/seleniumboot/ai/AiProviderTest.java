package com.seleniumboot.ai;

import com.seleniumboot.config.SeleniumBootConfig;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Drives each AI provider against a local fake endpoint — no network, no real key. */
public class AiProviderTest {

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<com.sun.net.httpserver.Headers> headers = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = "{}";

    @BeforeMethod
    public void start() throws Exception {
        status = 200;
        reply = "{}";
        path.set(null);
        body.set(null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            headers.set(ex.getRequestHeaders());
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
    }

    @AfterMethod
    public void stop() {
        server.stop(0);
    }

    private SeleniumBootConfig.Ai cfg(String provider, String model) {
        SeleniumBootConfig.Ai ai = new SeleniumBootConfig.Ai();
        ai.setProvider(provider);
        ai.setModel(model);
        ai.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        ai.setTimeoutSeconds(5);
        return ai;
    }

    @Test
    public void anthropicIsTheDefaultAndUsesItsHeadersAndModel() {
        reply = "{\"content\":[{\"type\":\"text\",\"text\":\"claude says hi\"}]}";
        SeleniumBootConfig.Ai ai = cfg(null, null);

        Assert.assertEquals(AiFailureAnalyzer.callApi(ai, "k1", "hello \"world\"\nline2"), "claude says hi");
        Assert.assertEquals(path.get(), "/api/messages");
        Assert.assertEquals(headers.get().getFirst("x-api-key"), "k1");
        Assert.assertNotNull(headers.get().getFirst("anthropic-version"));
        Assert.assertTrue(body.get().contains("\"model\":\"claude-haiku-4-5-20251001\""));
        Assert.assertTrue(body.get().contains("hello \\\"world\\\"\\nline2"));
    }

    @Test
    public void geminiUsesGenerateContentAndKeyHeader() {
        reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"gemini says hi\"}]}}]}";
        SeleniumBootConfig.Ai ai = cfg("gemini", "gemini-2.5-flash");

        Assert.assertEquals(AiFailureAnalyzer.callApi(ai, "g1", "ping"), "gemini says hi");
        Assert.assertEquals(path.get(), "/api/models/gemini-2.5-flash:generateContent");
        Assert.assertEquals(headers.get().getFirst("x-goog-api-key"), "g1");
        Assert.assertTrue(body.get().contains("\"parts\":[{\"text\":\"ping\"}]"));
    }

    @Test
    public void geminiDefaultModelIsTheLatestAlias() {
        reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}";
        Assert.assertEquals(AiFailureAnalyzer.callApi(cfg("gemini", null), "g1", "ping"), "ok");
        Assert.assertEquals(path.get(), "/api/models/gemini-flash-latest:generateContent");
    }

    @Test
    public void openAiCompatibleUsesChatCompletionsAndBearerAuth() {
        reply = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"gpt says hi\"}}]}";
        SeleniumBootConfig.Ai ai = cfg("openai", null);

        Assert.assertEquals(AiFailureAnalyzer.callApi(ai, "o1", "ping"), "gpt says hi");
        Assert.assertEquals(path.get(), "/api/chat/completions");
        Assert.assertEquals(headers.get().getFirst("authorization"), "Bearer o1");
        Assert.assertTrue(body.get().contains("\"model\":\"gpt-4o-mini\""));
    }

    @Test
    public void providerNameIsCaseInsensitiveAndAliased() {
        reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}";
        Assert.assertEquals(AiFailureAnalyzer.callApi(cfg("Google", "m"), "k", "p"), "ok");
        Assert.assertEquals(path.get(), "/api/models/m:generateContent");
    }

    @Test
    public void unknownProviderIsSkippedWithoutCallingOut() {
        Assert.assertNull(AiFailureAnalyzer.callApi(cfg("bogus", null), "k", "p"));
        Assert.assertNull(path.get());
    }

    @Test
    public void httpErrorReturnsNull() {
        status = 401;
        reply = "{\"error\":\"bad key\"}";
        Assert.assertNull(AiFailureAnalyzer.callApi(cfg("anthropic", null), "k", "p"));
    }
}
