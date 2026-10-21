package com.seleniumboot.ai;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.metrics.ExecutionMetrics;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/** One AI call per distinct failure cause, against a local fake provider. */
public class AiFailureAnalyzerSameCauseTest {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int status;
    private volatile long delayMs;

    @BeforeMethod
    public void start() throws Exception {
        calls.set(0);
        status = 200;
        delayMs = 0;
        AiFailureAnalyzer.resetCache();
        ExecutionMetrics.reset();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            calls.incrementAndGet();
            ex.getRequestBody().readAllBytes();
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { }
            byte[] out = "{\"content\":[{\"type\":\"text\",\"text\":\"Root cause: login API down\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
    }

    @AfterMethod
    public void stop() {
        server.stop(0);
        AiFailureAnalyzer.resetCache();
        ExecutionMetrics.reset();
    }

    private SeleniumBootConfig.Ai cfg() {
        SeleniumBootConfig.Ai ai = new SeleniumBootConfig.Ai();
        ai.setFailureAnalysis(true);
        ai.setApiKey("k");
        ai.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        ai.setTimeoutSeconds(5);
        return ai;
    }

    private static void failed(String id, String message) {
        ExecutionMetrics.recordBrowser(id, "chrome");
        ExecutionMetrics.recordError(id, new AssertionError(message));
    }

    @Test
    public void sameCauseFailures_shareOneCall_andEachGetTheAnalysis() {
        failed("a.One.run", "login API returned 500");
        failed("a.Two.run", "login API returned 503");
        failed("a.Three.run", "login API returned 502");

        for (String id : new String[]{"a.One.run", "a.Two.run", "a.Three.run"}) {
            AiFailureAnalyzer.analyze(cfg(), id, null, null);
        }

        assertEquals(calls.get(), 1, "numbers normalise, so one cause costs one call");
        for (String id : new String[]{"a.One.run", "a.Two.run", "a.Three.run"}) {
            assertEquals(ExecutionMetrics.getTiming(id).getAiAnalysis(), "Root cause: login API down", id);
        }
    }

    @Test
    public void differentCauses_getTheirOwnCall() {
        failed("a.One.run", "login API returned 500");
        failed("a.Other.run", "cart badge missing");

        AiFailureAnalyzer.analyze(cfg(), "a.One.run", null, null);
        AiFailureAnalyzer.analyze(cfg(), "a.Other.run", null, null);

        assertEquals(calls.get(), 2);
    }

    @Test
    public void concurrentSameCauseFailures_waitForTheFirstCall() throws Exception {
        delayMs = 300;
        int n = 4;
        for (int i = 0; i < n; i++) failed("a.T" + i + ".run", "login API returned 500");

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            String id = "a.T" + i + ".run";
            pool.submit(() -> {
                go.await();
                AiFailureAnalyzer.analyze(cfg(), id, null, null);
                return null;
            });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));

        assertEquals(calls.get(), 1, "concurrent same-cause failures share the in-flight call");
        for (int i = 0; i < n; i++) {
            assertNotNull(ExecutionMetrics.getTiming("a.T" + i + ".run").getAiAnalysis());
        }
    }

    @Test
    public void failedCall_isNotCached_soTheNextSameCauseFailureRetries() {
        failed("a.One.run", "login API returned 500");
        failed("a.Two.run", "login API returned 500");

        status = 500;
        AiFailureAnalyzer.analyze(cfg(), "a.One.run", null, null);
        assertNull(ExecutionMetrics.getTiming("a.One.run").getAiAnalysis());

        status = 200;
        AiFailureAnalyzer.analyze(cfg(), "a.Two.run", null, null);

        assertEquals(calls.get(), 2, "an error must not poison the cause");
        assertNotNull(ExecutionMetrics.getTiming("a.Two.run").getAiAnalysis());
    }
}
