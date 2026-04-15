package com.seleniumboot.network;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v144.fetch.Fetch;
import org.openqa.selenium.devtools.v144.fetch.model.HeaderEntry;
import org.openqa.selenium.devtools.v144.fetch.model.RequestId;
import org.openqa.selenium.devtools.v144.fetch.model.RequestPattern;
import org.openqa.selenium.devtools.v144.fetch.model.RequestStage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * CDP-backed network interception and request mocking.
 *
 * <p>Supported on Chrome and Edge (Chromium-based). On Firefox or other browsers
 * a warning is logged and all stub calls are silently ignored — tests continue
 * without interception.
 *
 * <p>Obtain via {@code networkMock()} in {@link com.seleniumboot.test.BasePage}
 * or {@link com.seleniumboot.test.BaseTest}.
 *
 * <pre>
 * // Return mock JSON for an API call
 * networkMock().stub("** /api/users").returnJson("{\"users\":[{\"id\":1}]}");
 *
 * // Simulate a server error
 * networkMock().stub("** /api/checkout").returnStatus(500);
 *
 * // Simulate slow network (2-second delay)
 * networkMock().stub("** /api/products").delay(2000).returnJson("[]");
 *
 * // Clear all stubs (auto-called after each test by the framework)
 * networkMock().clear();
 * </pre>
 */
@SeleniumBootApi(since = "1.5.0")
public final class NetworkMock {

    private static final Logger LOG = Logger.getLogger(NetworkMock.class.getName());

    private static final ThreadLocal<NetworkMock> INSTANCE =
            ThreadLocal.withInitial(NetworkMock::new);

    private final List<StubBuilder> stubs = new CopyOnWriteArrayList<>();
    private boolean cdpAttached = false;
    private DevTools devTools;

    private NetworkMock() {
    }

    /** Returns the per-thread {@link NetworkMock} instance. */
    public static NetworkMock get() {
        return INSTANCE.get();
    }

    /** Removes the per-thread instance (called by the framework after each test). */
    public static void cleanup() {
        NetworkMock mock = INSTANCE.get();
        if (mock != null) mock.clear();
        INSTANCE.remove();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Begins a stub for requests whose URL matches the given pattern.
     *
     * <p>Pattern syntax:
     * <ul>
     *   <li>{@code *}  — matches any characters except {@code /}</li>
     *   <li>{@code **} — matches any characters including {@code /}</li>
     *   <li>Exact URL — e.g. {@code https://api.example.com/users}</li>
     * </ul>
     *
     * @param urlPattern glob-style URL pattern
     * @return a {@link StubBuilder} to configure the response
     */
    public StubBuilder stub(String urlPattern) {
        return new StubBuilder(this, urlPattern);
    }

    /**
     * Removes all registered stubs and disables CDP interception for this thread.
     * Called automatically by the framework after each test.
     */
    public void clear() {
        stubs.clear();
        if (devTools != null) {
            try {
                devTools.send(Fetch.disable());
            } catch (Exception ignored) {
                // DevTools session may already be closed
            }
            devTools = null;
        }
        cdpAttached = false;
    }

    // ------------------------------------------------------------------
    // Internal — called by StubBuilder
    // ------------------------------------------------------------------

    void register(StubBuilder stub) {
        stubs.add(stub);
        ensureCdpAttached();
    }

    // ------------------------------------------------------------------
    // CDP wiring
    // ------------------------------------------------------------------

    private void ensureCdpAttached() {
        if (cdpAttached) return;

        WebDriver driver = DriverManager.getDriver();
        if (!(driver instanceof ChromiumDriver)) {
            LOG.warning("[Selenium Boot] NetworkMock requires Chrome/Edge. " +
                    "Stubs will be ignored on " + driver.getClass().getSimpleName() + ".");
            cdpAttached = true;
            return;
        }

        devTools = ((ChromiumDriver) driver).getDevTools();
        devTools.createSession();

        // Enable Fetch domain — intercept all requests before they are sent
        List<RequestPattern> patterns = List.of(
                new RequestPattern(
                        Optional.of("*"),
                        Optional.empty(),
                        Optional.of(RequestStage.REQUEST)
                )
        );
        devTools.send(Fetch.enable(Optional.of(patterns), Optional.empty()));

        // Handle each intercepted request
        devTools.addListener(Fetch.requestPaused(), event -> {
            String    url       = event.getRequest().getUrl();
            RequestId requestId = event.getRequestId();

            StubBuilder match = findMatch(url);

            if (match == null) {
                // No stub — let the request continue normally
                devTools.send(Fetch.continueRequest(
                        requestId,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                ));
                return;
            }

            // Apply delay if configured
            if (match.delayMs > 0) {
                try { Thread.sleep(match.delayMs); } catch (InterruptedException ignored) {}
            }

            // Build response body as base64
            String body64 = Base64.getEncoder().encodeToString(
                    match.responseBody.getBytes(StandardCharsets.UTF_8));

            List<HeaderEntry> headers = List.of(
                    new HeaderEntry("content-type", match.contentType)
            );

            devTools.send(Fetch.fulfillRequest(
                    requestId,
                    match.statusCode,
                    Optional.of(headers),
                    Optional.empty(),
                    Optional.of(body64),
                    Optional.empty()
            ));
        });

        cdpAttached = true;
    }

    // ------------------------------------------------------------------
    // Pattern matching
    // ------------------------------------------------------------------

    private StubBuilder findMatch(String url) {
        for (StubBuilder stub : stubs) {
            if (matches(stub.pattern, url)) return stub;
        }
        return null;
    }

    /**
     * Glob-style URL matching.
     * {@code **} matches anything including slashes; {@code *} matches within a path segment.
     */
    public static boolean matches(String pattern, String url) {
        return url.matches(globToRegex(pattern));
    }

    public static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("(?i)");
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                sb.append(".*");
                i += 2;
                if (i < glob.length() && glob.charAt(i) == '/') i++; // skip trailing /
            } else if (c == '*') {
                sb.append("[^/]*");
                i++;
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else {
                sb.append(java.util.regex.Pattern.quote(String.valueOf(c)));
                i++;
            }
        }
        return sb.toString();
    }
}
