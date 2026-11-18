package com.seleniumboot.tracing;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** #259: CDP events for one request can be handled out of order. */
public class RequestCorrelatorTest {

    @Test
    public void inOrder_runsWithTheRequest() {
        RequestCorrelator c = new RequestCorrelator();
        List<RequestCorrelator.Request> got = new ArrayList<>();
        c.requestSent("1", "POST", "http://x/fail");
        c.whenKnown("1", got::add);
        assertEquals(got, List.of(new RequestCorrelator.Request("POST", "http://x/fail")));
    }

    @Test
    public void responseBeforeRequest_waitsAndKeepsMethodAndUrl() {
        RequestCorrelator c = new RequestCorrelator();
        List<RequestCorrelator.Request> got = new ArrayList<>();
        c.whenKnown("1", got::add);          // responseReceived handled first
        assertTrue(got.isEmpty(), "must not run before the request is known");
        c.requestSent("1", "POST", "http://x/fail");
        assertEquals(got, List.of(new RequestCorrelator.Request("POST", "http://x/fail")));
        c.flush();
        assertEquals(got.size(), 1, "an action runs once");
    }

    @Test
    public void requestNeverArrives_flushRunsWithNull() {
        RequestCorrelator c = new RequestCorrelator();
        List<RequestCorrelator.Request> got = new ArrayList<>();
        c.whenKnown("1", got::add);
        c.flush();
        assertEquals(got.size(), 1);
        assertNull(got.get(0));
    }

    @Test
    public void concurrentDelivery_everyActionSeesItsRequest() throws Exception {
        RequestCorrelator c = new RequestCorrelator();
        List<RequestCorrelator.Request> got = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(8);
        int n = 2000;
        CountDownLatch go = new CountDownLatch(1);
        try {
            for (int i = 0; i < n; i++) {
                String id = String.valueOf(i);
                pool.execute(() -> { await(go); c.whenKnown(id, got::add); });
                pool.execute(() -> { await(go); c.requestSent(id, "POST", "http://x/" + id); });
            }
            go.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        c.flush();
        assertEquals(got.size(), n);
        assertTrue(got.stream().allMatch(r -> r != null && "POST".equals(r.method())), "lost a request");
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
