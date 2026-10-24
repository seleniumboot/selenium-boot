package com.seleniumboot.network;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.BytesValue;
import org.openqa.selenium.bidi.network.ContinueRequestParameters;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.ProvideResponseParameters;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v153.fetch.Fetch;
import org.openqa.selenium.devtools.v153.fetch.model.HeaderEntry;
import org.openqa.selenium.devtools.v153.fetch.model.RequestId;
import org.openqa.selenium.devtools.v153.fetch.model.RequestPattern;
import org.openqa.selenium.devtools.v153.fetch.model.RequestPaused;
import org.openqa.selenium.devtools.v153.fetch.model.RequestStage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Network interception and request mocking backed by WebDriver BiDi and CDP.
 *
 * <p>Supported on Chrome, Edge, and Firefox. On drivers supporting {@link HasBiDi}
 * (including Firefox and BiDi-enabled Chromium), WebDriver BiDi is used under the hood;
 * otherwise, it falls back to Chrome DevTools Protocol (CDP) for {@link ChromiumDriver}.
 * On unsupported browsers without BiDi or CDP, a warning is logged and all stub calls
 * are silently ignored — tests continue without interception.
 *
 * <p>Obtain via {@code networkMock()} in {@link com.seleniumboot.test.BasePage}
 * or {@link com.seleniumboot.test.BaseTest}.
 *
 * <pre>
 * // Return mock JSON for an API call
 * networkMock().stub("**&#47;api/users").returnJson("{\"users\":[{\"id\":1}]}");
 *
 * // Simulate a server error
 * networkMock().stub("**&#47;api/checkout").returnStatus(500);
 *
 * // Simulate slow network (2-second delay)
 * networkMock().stub("**&#47;api/products").delay(2000).returnJson("[]");
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

    @FunctionalInterface
    interface BiDiNetworkFactory {
        Network create(WebDriver driver);
    }

    // Factory hook for testing BiDi without an active browser WebSocket session
    static BiDiNetworkFactory bidiFactory = Network::new;

    private final List<StubBuilder> stubs = new CopyOnWriteArrayList<>();
    private boolean attached = false;
    private DevTools devTools;
    private Network bidiNetwork;
    private String bidiInterceptId;

    NetworkMock() {
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
     * Removes all registered stubs and disables interception for this thread.
     * Called automatically by the framework after each test.
     */
    public void clear() {
        stubs.clear();

        if (bidiNetwork != null) {
            try {
                if (bidiInterceptId != null) {
                    bidiNetwork.removeIntercept(bidiInterceptId);
                }
            } catch (Exception ignored) {
                // BiDi session may already be closed
            }
            try {
                bidiNetwork.close();
            } catch (Exception ignored) {
                // BiDi session may already be closed
            }
            bidiNetwork = null;
            bidiInterceptId = null;
        }

        if (devTools != null) {
            try {
                devTools.send(Fetch.disable());
            } catch (Exception ignored) {
                // DevTools session may already be closed
            }
            devTools = null;
        }

        attached = false;
    }

    // ------------------------------------------------------------------
    // Internal — called by StubBuilder
    // ------------------------------------------------------------------

    void register(StubBuilder stub) {
        stubs.add(stub);
        ensureAttached();
    }

    // ------------------------------------------------------------------
    // Interception wiring (BiDi with CDP fallback)
    // ------------------------------------------------------------------

    private void ensureAttached() {
        if (attached) return;

        WebDriver driver;
        try {
            driver = DriverManager.getDriver();
        } catch (IllegalStateException e) {
            LOG.warning("[Selenium Boot] No active WebDriver session found for NetworkMock. " +
                    "Stubs will be ignored.");
            attached = true;
            return;
        }

        if (driver == null) {
            LOG.warning("[Selenium Boot] No active WebDriver session found for NetworkMock. " +
                    "Stubs will be ignored.");
            attached = true;
            return;
        }

        // 1. Try BiDi first if driver supports HasBiDi
        if (driver instanceof HasBiDi) {
            try {
                bidiNetwork = bidiFactory.create(driver);
                bidiInterceptId = bidiNetwork.addIntercept(
                        new AddInterceptParameters(InterceptPhase.BEFORE_REQUEST_SENT)
                );
                bidiNetwork.onBeforeRequestSent(this::handleBiDiRequest);
                attached = true;
                return;
            } catch (Exception e) {
                LOG.fine("[Selenium Boot] BiDi network interception unavailable (" + e.getMessage() +
                        "); attempting CDP fallback.");
                if (bidiNetwork != null) {
                    try { bidiNetwork.close(); } catch (Exception ignored) {}
                    bidiNetwork = null;
                }
                bidiInterceptId = null;
            }
        }

        // 2. Fall back to CDP for Chromium-based drivers
        if (driver instanceof ChromiumDriver chromium) {
            try {
                devTools = chromium.getDevTools();
                devTools.createSession();

                List<RequestPattern> patterns = List.of(
                        new RequestPattern(
                                Optional.of("*"),
                                Optional.empty(),
                                Optional.of(RequestStage.REQUEST)
                        )
                );
                devTools.send(Fetch.enable(Optional.of(patterns), Optional.empty()));
                devTools.addListener(Fetch.requestPaused(), this::handleCdpRequest);
                attached = true;
                return;
            } catch (Exception e) {
                LOG.fine("[Selenium Boot] CDP interception initialization failed: " + e.getMessage());
                devTools = null;
            }
        }

        // 3. Neither BiDi nor CDP supported
        LOG.warning("[Selenium Boot] NetworkMock requires BiDi (Firefox/Chrome) or CDP (Chrome/Edge). " +
                "Stubs will be ignored on " + driver.getClass().getSimpleName() + ".");
        attached = true;
    }

    // ------------------------------------------------------------------
    // Request handlers
    // ------------------------------------------------------------------

    void handleBiDiRequest(BeforeRequestSent event) {
        if (!event.isBlocked()) {
            return;
        }

        String    url       = event.getRequest().getUrl();
        String    requestId = event.getRequest().getRequestId();
        StubBuilder match   = findMatch(url);

        try {
            if (match == null) {
                bidiNetwork.continueRequest(new ContinueRequestParameters(requestId));
                return;
            }

            if (match.delayMs > 0) {
                try {
                    Thread.sleep(match.delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            String contentType = match.contentType != null ? match.contentType : "application/json";
            List<Header> headers = List.of(
                    new Header("content-type", new BytesValue(BytesValue.Type.STRING, contentType))
            );

            String body = match.responseBody != null ? match.responseBody : "";
            ProvideResponseParameters params = new ProvideResponseParameters(requestId)
                    .statusCode(match.statusCode)
                    .headers(headers)
                    .body(new BytesValue(BytesValue.Type.STRING, body));

            bidiNetwork.provideResponse(params);
        } catch (Exception e) {
            LOG.warning("[Selenium Boot] Failed to process BiDi intercepted request for " + url + ": " + e.getMessage());
        }
    }

    void handleCdpRequest(RequestPaused event) {
        String    url       = event.getRequest().getUrl();
        RequestId requestId = event.getRequestId();

        try {
            StubBuilder match = findMatch(url);

            if (match == null) {
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

            if (match.delayMs > 0) {
                try {
                    Thread.sleep(match.delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            String body = match.responseBody != null ? match.responseBody : "";
            String body64 = Base64.getEncoder().encodeToString(
                    body.getBytes(StandardCharsets.UTF_8));

            String contentType = match.contentType != null ? match.contentType : "application/json";
            List<HeaderEntry> headers = List.of(
                    new HeaderEntry("content-type", contentType)
            );

            devTools.send(Fetch.fulfillRequest(
                    requestId,
                    match.statusCode,
                    Optional.of(headers),
                    Optional.empty(),
                    Optional.of(body64),
                    Optional.empty()
            ));
        } catch (Exception e) {
            LOG.warning("[Selenium Boot] Failed to process CDP intercepted request for " + url + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Testing / Inspection helpers (package-private for test support)
    // ------------------------------------------------------------------

    static void setBiDiFactory(BiDiNetworkFactory factory) {
        bidiFactory = factory;
    }

    static void resetBiDiFactory() {
        bidiFactory = Network::new;
    }

    boolean isAttached() {
        return attached;
    }

    boolean isBiDiActive() {
        return bidiNetwork != null;
    }

    boolean isCdpActive() {
        return devTools != null;
    }

    List<StubBuilder> getStubs() {
        return Collections.unmodifiableList(stubs);
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
