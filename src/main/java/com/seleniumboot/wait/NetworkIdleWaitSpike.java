package com.seleniumboot.wait;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v153.network.model.LoadingFailed;
import org.openqa.selenium.devtools.v153.network.model.LoadingFinished;
import org.openqa.selenium.devtools.v153.network.model.RequestWillBeSent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SPIKE — not the public API. Prototype for the design note in ROADMAP.md
 * (waitForNetworkIdle / waitForResponse): CDP on Chromium, BiDi on Firefox, throw on others.
 *
 * <p>Mechanism on both paths is the same shape: track an in-flight request counter and
 * a "last activity" timestamp; idle means the counter is 0 and {@code quietFor} has
 * elapsed since the last change, bounded by an overall timeout.
 *
 * <p>Manual run only (no browser in the unit-test suite) — see the {@code main} method.
 */
public final class NetworkIdleWaitSpike {

    private NetworkIdleWaitSpike() {
    }

    /**
     * Blocks until no network request has started or finished for {@code quietFor},
     * or throws once {@code timeout} elapses first.
     */
    public static void waitForNetworkIdle(WebDriver driver, Duration quietFor, Duration timeout) {
        if (driver instanceof ChromiumDriver chromium) {
            waitViaCdp(chromium, quietFor, timeout);
        } else if (driver instanceof HasBiDi) {
            waitViaBidi(driver, quietFor, timeout);
        } else {
            throw new UnsupportedOperationException(
                    "waitForNetworkIdle needs CDP (Chrome/Edge) or BiDi support; got "
                            + driver.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // CDP path (Chrome / Edge)
    // ------------------------------------------------------------------

    private static void waitViaCdp(ChromiumDriver driver, Duration quietFor, Duration timeout) {
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicLongBox lastActivity = new AtomicLongBox(System.nanoTime());

        try (DevTools devTools = driver.getDevTools()) {
            devTools.createSession();
            devTools.send(org.openqa.selenium.devtools.v153.network.Network.enable(
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

            devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.requestWillBeSent(),
                    (RequestWillBeSent event) -> {
                        inFlight.incrementAndGet();
                        lastActivity.set(System.nanoTime());
                    });
            devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.loadingFinished(),
                    (LoadingFinished event) -> {
                        inFlight.decrementAndGet();
                        lastActivity.set(System.nanoTime());
                    });
            devTools.addListener(org.openqa.selenium.devtools.v153.network.Network.loadingFailed(),
                    (LoadingFailed event) -> {
                        inFlight.decrementAndGet();
                        lastActivity.set(System.nanoTime());
                    });

            pollUntilIdle(inFlight, lastActivity, quietFor, timeout);
        }
    }

    // ------------------------------------------------------------------
    // BiDi path (Firefox, and any other BiDi-capable browser)
    // ------------------------------------------------------------------

    private static void waitViaBidi(WebDriver driver, Duration quietFor, Duration timeout) {
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicLongBox lastActivity = new AtomicLongBox(System.nanoTime());

        try (Network network = new Network(driver)) {
            network.onBeforeRequestSent(event -> {
                inFlight.incrementAndGet();
                lastActivity.set(System.nanoTime());
            });
            network.onResponseCompleted(event -> {
                inFlight.decrementAndGet();
                lastActivity.set(System.nanoTime());
            });
            network.onFetchError(event -> {
                inFlight.decrementAndGet();
                lastActivity.set(System.nanoTime());
            });

            pollUntilIdle(inFlight, lastActivity, quietFor, timeout);
        }
    }

    // ------------------------------------------------------------------
    // Shared poll loop
    // ------------------------------------------------------------------

    private static void pollUntilIdle(AtomicInteger inFlight, AtomicLongBox lastActivity,
                                        Duration quietFor, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            long quietNanos = System.nanoTime() - lastActivity.get();
            if (inFlight.get() <= 0 && quietNanos >= quietFor.toNanos()) {
                return;
            }
            sleep(100);
        }
        throw new RuntimeException("waitForNetworkIdle: timed out after " + timeout
                + " with " + inFlight.get() + " request(s) still in flight");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Tiny mutable long box — avoids AtomicLong just to store a nanoTime snapshot. */
    private static final class AtomicLongBox {
        private volatile long value;

        AtomicLongBox(long value) {
            this.value = value;
        }

        void set(long value) {
            this.value = value;
        }

        long get() {
            return value;
        }
    }

    // ------------------------------------------------------------------
    // Manual spike runner — not a unit test, needs a real browser
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        String target = args.length > 0 ? args[0] : "chrome";
        WebDriver driver = target.equals("firefox")
                ? new org.openqa.selenium.firefox.FirefoxDriver()
                : new org.openqa.selenium.chrome.ChromeDriver();
        try {
            driver.get("https://example.com");
            long start = System.nanoTime();
            waitForNetworkIdle(driver, Duration.ofMillis(500), Duration.ofSeconds(10));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[" + target + "] network idle reached after " + elapsedMs + "ms");
        } finally {
            driver.quit();
        }
    }
}
