package com.seleniumboot.wait;

import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.*;

/**
 * Unit tests for the pure request-tracking logic behind {@link WaitEngine#waitForNetworkIdle}
 * and {@link WaitEngine#waitForResponse}. CDP/BiDi wiring itself needs a real browser and is
 * verified manually — same convention as {@code NetworkMockTest} for CDP request interception.
 */
public class NetworkActivityTrackerTest {

    // ------------------------------------------------------------------
    // IdleState
    // ------------------------------------------------------------------

    @Test
    public void idleState_startsIdle() {
        NetworkActivityTracker.IdleState state = new NetworkActivityTracker.IdleState();
        assertTrue(state.isIdle(Duration.ZERO));
    }

    @Test
    public void idleState_notIdleWhileRequestInFlight() {
        NetworkActivityTracker.IdleState state = new NetworkActivityTracker.IdleState();
        state.requestStarted();
        assertFalse(state.isIdle(Duration.ZERO));
        assertEquals(state.inFlightCount(), 1);
    }

    @Test
    public void idleState_idleAgainOnceRequestEnds() {
        NetworkActivityTracker.IdleState state = new NetworkActivityTracker.IdleState();
        state.requestStarted();
        state.requestEnded();
        assertTrue(state.isIdle(Duration.ZERO));
        assertEquals(state.inFlightCount(), 0);
    }

    @Test
    public void idleState_notIdleUntilQuietPeriodElapses() throws InterruptedException {
        NetworkActivityTracker.IdleState state = new NetworkActivityTracker.IdleState();
        state.requestStarted();
        state.requestEnded();
        // just ended — a long quiet period hasn't elapsed yet
        assertFalse(state.isIdle(Duration.ofMinutes(1)));
        Thread.sleep(20);
        assertTrue(state.isIdle(Duration.ofMillis(5)));
    }

    @Test
    public void idleState_multipleOverlappingRequests() {
        NetworkActivityTracker.IdleState state = new NetworkActivityTracker.IdleState();
        state.requestStarted();
        state.requestStarted();
        assertEquals(state.inFlightCount(), 2);
        state.requestEnded();
        assertFalse(state.isIdle(Duration.ZERO), "one request still in flight");
        state.requestEnded();
        assertTrue(state.isIdle(Duration.ZERO));
    }

    // ------------------------------------------------------------------
    // ResponseLatch
    // ------------------------------------------------------------------

    @Test
    public void responseLatch_returnsStatusOnceCompleted() {
        NetworkActivityTracker.ResponseLatch latch = new NetworkActivityTracker.ResponseLatch();
        latch.complete(204);
        assertEquals(latch.await(Duration.ofSeconds(1)), 204);
    }

    @Test
    public void responseLatch_returnsStatusFromAnotherThread() throws InterruptedException {
        NetworkActivityTracker.ResponseLatch latch = new NetworkActivityTracker.ResponseLatch();
        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
            latch.complete(200);
        });
        producer.start();
        assertEquals(latch.await(Duration.ofSeconds(1)), 200);
        producer.join();
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void responseLatch_timesOutWithNoMatch() {
        NetworkActivityTracker.ResponseLatch latch = new NetworkActivityTracker.ResponseLatch();
        latch.await(Duration.ofMillis(50));
    }
}
