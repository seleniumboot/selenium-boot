package com.seleniumboot.wait;

import com.seleniumboot.network.NetworkMock;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v153.network.model.LoadingFailed;
import org.openqa.selenium.devtools.v153.network.model.LoadingFinished;
import org.openqa.selenium.devtools.v153.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v153.network.model.ResponseReceived;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Backs {@link WaitEngine#waitForNetworkIdle} and {@link WaitEngine#waitForResponse}.
 *
 * <p>CDP (Chrome/Edge) and BiDi (Firefox, or any other {@code HasBiDi} driver) wiring is
 * covered by manual verification against real browsers, not unit tests — same convention
 * as {@link NetworkMock}. {@link IdleState}, the poll condition both paths feed, is pure
 * and unit-tested directly.
 */
final class NetworkActivityTracker {

    private NetworkActivityTracker() {
    }

    static void awaitIdle(WebDriver driver, Duration quietFor, Duration timeout) {
        IdleState state = new IdleState();
        if (driver instanceof ChromiumDriver chromium) {
            try (DevTools devTools = chromium.getDevTools()) {
                devTools.createSession();
                devTools.send(org.openqa.selenium.devtools.v153.network.Network.enable(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.requestWillBeSent(),
                        (RequestWillBeSent event) -> state.requestStarted());
                devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.loadingFinished(),
                        (LoadingFinished event) -> state.requestEnded());
                devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.loadingFailed(),
                        (LoadingFailed event) -> state.requestEnded());
                pollUntilIdle(state, quietFor, timeout);
            }
        } else if (driver instanceof HasBiDi) {
            try (Network network = new Network(driver)) {
                network.onBeforeRequestSent(event -> state.requestStarted());
                network.onResponseCompleted(event -> state.requestEnded());
                network.onFetchError(event -> state.requestEnded());
                pollUntilIdle(state, quietFor, timeout);
            }
        } else {
            throw unsupported(driver);
        }
    }

    static int awaitResponse(WebDriver driver, String urlPattern, Duration timeout) {
        ResponseLatch latch = new ResponseLatch();
        if (driver instanceof ChromiumDriver chromium) {
            try (DevTools devTools = chromium.getDevTools()) {
                devTools.createSession();
                devTools.send(org.openqa.selenium.devtools.v153.network.Network.enable(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
                devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.responseReceived(),
                        (ResponseReceived event) -> {
                            String url = event.getResponse().getUrl();
                            if (NetworkMock.matches(urlPattern, url)) {
                                latch.complete(event.getResponse().getStatus());
                            }
                        });
                return latch.await(timeout);
            }
        } else if (driver instanceof HasBiDi) {
            try (Network network = new Network(driver)) {
                network.onResponseCompleted(event -> {
                    String url = event.getResponseData().getUrl();
                    if (NetworkMock.matches(urlPattern, url)) {
                        latch.complete(event.getResponseData().getStatus());
                    }
                });
                return latch.await(timeout);
            }
        } else {
            throw unsupported(driver);
        }
    }

    private static void pollUntilIdle(IdleState state, Duration quietFor, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (state.isIdle(quietFor)) return;
            sleep(100);
        }
        throw new TimeoutException("waitForNetworkIdle: timed out after " + timeout
                + " with " + state.inFlightCount() + " request(s) still in flight");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static UnsupportedOperationException unsupported(WebDriver driver) {
        return new UnsupportedOperationException(
                "waitForNetworkIdle/waitForResponse need CDP (Chrome/Edge) or BiDi support; got "
                        + driver.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------
    // Pure, unit-tested pieces
    // ------------------------------------------------------------------

    /** In-flight request counter + last-activity timestamp. Idle = none in flight, quiet long enough. */
    static final class IdleState {
        private final AtomicInteger inFlight = new AtomicInteger();
        private volatile long lastActivityNanos = System.nanoTime();

        void requestStarted() {
            inFlight.incrementAndGet();
            lastActivityNanos = System.nanoTime();
        }

        void requestEnded() {
            inFlight.decrementAndGet();
            lastActivityNanos = System.nanoTime();
        }

        boolean isIdle(Duration quietFor) {
            return inFlight.get() <= 0 && (System.nanoTime() - lastActivityNanos) >= quietFor.toNanos();
        }

        int inFlightCount() {
            return inFlight.get();
        }
    }

    /** Resolves once a matching response is observed, or times out. */
    static final class ResponseLatch {
        private final CompletableFuture<Integer> future = new CompletableFuture<>();

        void complete(int status) {
            future.complete(status);
        }

        int await(Duration timeout) {
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("waitForResponse: no matching response within " + timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TimeoutException("waitForResponse: interrupted while waiting", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("waitForResponse: listener failed", e.getCause());
            }
        }
    }
}
