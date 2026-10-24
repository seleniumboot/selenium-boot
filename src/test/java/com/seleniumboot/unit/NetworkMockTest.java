package com.seleniumboot.unit;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.network.NetworkMock;
import com.seleniumboot.network.NetworkMockTestSupport;
import com.seleniumboot.network.StubBuilder;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDiException;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.BeforeRequestSent;
import org.openqa.selenium.bidi.network.ContinueRequestParameters;
import org.openqa.selenium.bidi.network.Header;
import org.openqa.selenium.bidi.network.ProvideResponseParameters;
import org.openqa.selenium.bidi.network.RequestData;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.v153.fetch.Fetch;
import org.openqa.selenium.devtools.v153.fetch.model.HeaderEntry;
import org.openqa.selenium.devtools.v153.fetch.model.RequestId;
import org.openqa.selenium.devtools.v153.fetch.model.RequestPaused;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link NetworkMock} — URL matching, StubBuilder configurations,
 * WebDriver BiDi interception, CDP fallback, and unsupported browser handling.
 */
public class NetworkMockTest {

    @BeforeMethod
    public void setUp() {
        NetworkMock.cleanup();
        NetworkMockTestSupport.resetBiDiFactory();
    }

    @AfterMethod
    public void tearDown() {
        NetworkMock.cleanup();
        NetworkMockTestSupport.resetBiDiFactory();
        try {
            DriverManager.popSessionOverride();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Pattern matching — globToRegex + matches()
    // ------------------------------------------------------------------

    @Test
    public void matches_exactUrl() {
        assertTrue(NetworkMock.matches(
                "https://api.example.com/users",
                "https://api.example.com/users"));
    }

    @Test
    public void matches_doubleStarPrefix() {
        assertTrue(NetworkMock.matches("**/api/users", "https://api.example.com/api/users"));
        assertTrue(NetworkMock.matches("**/api/users", "http://localhost:8080/api/users"));
    }

    @Test
    public void matches_doubleStarSuffix() {
        assertTrue(NetworkMock.matches("https://api.example.com/**",
                "https://api.example.com/users"));
        assertTrue(NetworkMock.matches("https://api.example.com/**",
                "https://api.example.com/orders/123/items"));
    }

    @Test
    public void matches_singleStar_withinSegment() {
        assertTrue(NetworkMock.matches("https://api.example.com/users/*",
                "https://api.example.com/users/42"));
        assertFalse(NetworkMock.matches("https://api.example.com/users/*",
                "https://api.example.com/users/42/orders"));
    }

    @Test
    public void matches_doubleStarMiddle() {
        assertTrue(NetworkMock.matches("https://api.example.com/**/items",
                "https://api.example.com/orders/99/items"));
    }

    @Test
    public void noMatch_differentDomain() {
        assertFalse(NetworkMock.matches("**/api/users",
                "https://other.com/graphql"));
    }

    @Test
    public void noMatch_differentPath() {
        assertFalse(NetworkMock.matches("https://api.example.com/users",
                "https://api.example.com/orders"));
    }

    @Test
    public void matches_caseInsensitive() {
        assertTrue(NetworkMock.matches("**/API/Users",
                "https://api.example.com/api/users"));
    }

    @Test
    public void globToRegex_doubleStarConverted() {
        String regex = NetworkMock.globToRegex("**/api");
        assertTrue("https://host.com/api".matches(regex));
    }

    @Test
    public void globToRegex_singleStarConverted() {
        String regex = NetworkMock.globToRegex("https://host.com/users/*");
        assertTrue("https://host.com/users/42".matches(regex));
        assertFalse("https://host.com/users/42/details".matches(regex));
    }

    // ------------------------------------------------------------------
    // StubBuilder Configuration
    // ------------------------------------------------------------------

    @Test
    public void stub_returnJson_setsDefaults() {
        StubBuilder builder = NetworkMock.get().stub("**/users");
        builder.returnJson("{\"users\":[]}");

        assertEquals(builder.getPattern(), "**/users");
        assertEquals(builder.getResponseBody(), "{\"users\":[]}");
        assertEquals(builder.getContentType(), "application/json");
        assertEquals(builder.getStatusCode(), 200);
        assertEquals(builder.getDelayMs(), 0);
    }

    @Test
    public void stub_returnStatus_500() {
        StubBuilder builder = NetworkMock.get().stub("**/checkout");
        builder.returnStatus(500);

        assertEquals(builder.getStatusCode(), 500);
        assertEquals(builder.getResponseBody(), "");
    }

    @Test
    public void stub_returnStatus_withBody() {
        StubBuilder builder = NetworkMock.get().stub("**/error");
        builder.returnStatus(503, "Service Unavailable");

        assertEquals(builder.getStatusCode(), 503);
        assertEquals(builder.getResponseBody(), "Service Unavailable");
    }

    @Test
    public void stub_returnBody_customContentType() {
        StubBuilder builder = NetworkMock.get().stub("**/document.xml");
        builder.returnBody("<root/>", "application/xml");

        assertEquals(builder.getResponseBody(), "<root/>");
        assertEquals(builder.getContentType(), "application/xml");
        assertEquals(builder.getStatusCode(), 200);
    }

    @Test
    public void stub_delay_setsDelayMillis() {
        StubBuilder builder = NetworkMock.get().stub("**/slow").delay(1500);
        assertEquals(builder.getDelayMs(), 1500);
    }

    // ------------------------------------------------------------------
    // BiDi Interception Path
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void bidi_attachesAndInterceptsMatchingRequest() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        when(mockNetwork.addIntercept(any(AddInterceptParameters.class))).thenReturn("intercept-bidi-1");

        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("{\"users\":[{\"id\":1}]}");

        assertTrue(NetworkMockTestSupport.isBiDiActive(mock), "BiDi should be active when driver implements HasBiDi");
        assertTrue(NetworkMockTestSupport.isAttached(mock), "NetworkMock should be attached");
        verify(mockNetwork).addIntercept(any(AddInterceptParameters.class));

        ArgumentCaptor<Consumer<BeforeRequestSent>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockNetwork).onBeforeRequestSent(captor.capture());
        Consumer<BeforeRequestSent> listener = captor.getValue();

        // Simulate intercepted matching request
        BeforeRequestSent event = mock(BeforeRequestSent.class);
        RequestData req = mock(RequestData.class);
        when(event.isBlocked()).thenReturn(true);
        when(event.getRequest()).thenReturn(req);
        when(req.getUrl()).thenReturn("https://api.example.com/api/users");
        when(req.getRequestId()).thenReturn("req-101");

        listener.accept(event);

        ArgumentCaptor<ProvideResponseParameters> respCaptor =
                ArgumentCaptor.forClass(ProvideResponseParameters.class);
        verify(mockNetwork).provideResponse(respCaptor.capture());

        Map<String, Object> paramsMap = respCaptor.getValue().toMap();
        assertEquals(paramsMap.get("request"), "req-101");
        assertEquals(paramsMap.get("statusCode"), 200);

        List<Map<String, Object>> headers = (List<Map<String, Object>>) paramsMap.get("headers");
        assertNotNull(headers);
        assertEquals(headers.get(0).get("name"), "content-type");

        Map<String, String> bodyMap = (Map<String, String>) paramsMap.get("body");
        assertEquals(bodyMap.get("value"), "{\"users\":[{\"id\":1}]}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bidi_handlesNonMatchingRequest_callsContinueRequest() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        when(mockNetwork.addIntercept(any(AddInterceptParameters.class))).thenReturn("intercept-bidi-1");
        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("[]");

        ArgumentCaptor<Consumer<BeforeRequestSent>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockNetwork).onBeforeRequestSent(captor.capture());
        Consumer<BeforeRequestSent> listener = captor.getValue();

        // Non-matching URL
        BeforeRequestSent event = mock(BeforeRequestSent.class);
        RequestData req = mock(RequestData.class);
        when(event.isBlocked()).thenReturn(true);
        when(event.getRequest()).thenReturn(req);
        when(req.getUrl()).thenReturn("https://api.example.com/api/orders");
        when(req.getRequestId()).thenReturn("req-202");

        listener.accept(event);

        ArgumentCaptor<ContinueRequestParameters> contCaptor =
                ArgumentCaptor.forClass(ContinueRequestParameters.class);
        verify(mockNetwork).continueRequest(contCaptor.capture());
        assertEquals(contCaptor.getValue().toMap().get("request"), "req-202");
        verify(mockNetwork, never()).provideResponse(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bidi_ignoresUnblockedRequest() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        when(mockNetwork.addIntercept(any(AddInterceptParameters.class))).thenReturn("intercept-bidi-1");
        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("[]");

        ArgumentCaptor<Consumer<BeforeRequestSent>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockNetwork).onBeforeRequestSent(captor.capture());
        Consumer<BeforeRequestSent> listener = captor.getValue();

        BeforeRequestSent event = mock(BeforeRequestSent.class);
        when(event.isBlocked()).thenReturn(false);

        listener.accept(event);

        verify(mockNetwork, never()).provideResponse(any());
        verify(mockNetwork, never()).continueRequest(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bidi_delayAppliesWaitTime() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        when(mockNetwork.addIntercept(any(AddInterceptParameters.class))).thenReturn("intercept-bidi-1");
        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/slow").delay(40).returnStatus(200);

        ArgumentCaptor<Consumer<BeforeRequestSent>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockNetwork).onBeforeRequestSent(captor.capture());
        Consumer<BeforeRequestSent> listener = captor.getValue();

        BeforeRequestSent event = mock(BeforeRequestSent.class);
        RequestData req = mock(RequestData.class);
        when(event.isBlocked()).thenReturn(true);
        when(event.getRequest()).thenReturn(req);
        when(req.getUrl()).thenReturn("https://api.example.com/slow");
        when(req.getRequestId()).thenReturn("req-slow");

        long start = System.currentTimeMillis();
        listener.accept(event);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 30, "Should have delayed response by at least 30ms, took " + elapsed + "ms");
        verify(mockNetwork).provideResponse(any());
    }

    @Test
    public void bidi_clear_removesInterceptAndClosesNetwork() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        when(mockNetwork.addIntercept(any(AddInterceptParameters.class))).thenReturn("intercept-bidi-1");
        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("[]");

        mock.clear();

        verify(mockNetwork).removeIntercept("intercept-bidi-1");
        verify(mockNetwork).close();
        assertFalse(NetworkMockTestSupport.isBiDiActive(mock));
        assertFalse(NetworkMockTestSupport.isAttached(mock));
        assertTrue(NetworkMockTestSupport.getStubs(mock).isEmpty());
    }

    // ------------------------------------------------------------------
    // CDP Fallback Paths
    // ------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void fallback_whenBiDiFailsOnChromium_fallsBackToCdp() {
        // Driver supports both HasBiDi and ChromiumDriver, but BiDi fails
        ChromiumDriver chromium = mock(ChromiumDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DevTools devTools = mock(DevTools.class);
        when(chromium.getDevTools()).thenReturn(devTools);
        DriverManager.pushSessionOverride(chromium);

        NetworkMockTestSupport.setBiDiFactory(d -> {
            throw new BiDiException("WebSocket connection rejected");
        });

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/checkout").returnStatus(500);

        assertFalse(NetworkMockTestSupport.isBiDiActive(mock), "BiDi should be inactive due to failure");
        assertTrue(NetworkMockTestSupport.isCdpActive(mock), "CDP should be active after BiDi fallback");
        assertTrue(NetworkMockTestSupport.isAttached(mock), "NetworkMock should be attached");

        verify(devTools).createSession();
        verify(devTools).send(any(Command.class));

        ArgumentCaptor<Consumer<RequestPaused>> cdpCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(devTools).addListener(any(), cdpCaptor.capture());
        Consumer<RequestPaused> cdpListener = cdpCaptor.getValue();

        RequestPaused event = mock(RequestPaused.class);
        org.openqa.selenium.devtools.v153.network.model.Request cdpReq =
                mock(org.openqa.selenium.devtools.v153.network.model.Request.class);
        when(event.getRequest()).thenReturn(cdpReq);
        when(cdpReq.getUrl()).thenReturn("https://api.example.com/api/checkout");
        when(event.getRequestId()).thenReturn(new RequestId("cdp-1"));

        cdpListener.accept(event);

        verify(devTools, atLeastOnce()).send(any(Command.class));

        mock.clear();
        assertFalse(NetworkMockTestSupport.isCdpActive(mock));
        assertFalse(NetworkMockTestSupport.isAttached(mock));
    }

    @Test
    public void fallback_whenDriverIsOnlyChromium_attachesCdpDirectly() {
        ChromiumDriver chromium = mock(ChromiumDriver.class);
        DevTools devTools = mock(DevTools.class);
        when(chromium.getDevTools()).thenReturn(devTools);
        DriverManager.pushSessionOverride(chromium);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/products").returnJson("[]");

        assertFalse(NetworkMockTestSupport.isBiDiActive(mock));
        assertTrue(NetworkMockTestSupport.isCdpActive(mock));
        assertTrue(NetworkMockTestSupport.isAttached(mock));
        verify(devTools).createSession();
    }

    @Test
    public void fallback_unsupportedBrowser_logsWarningAndContinuesSafely() {
        WebDriver unsupportedDriver = mock(WebDriver.class);
        DriverManager.pushSessionOverride(unsupportedDriver);

        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("[]");

        assertFalse(NetworkMockTestSupport.isBiDiActive(mock));
        assertFalse(NetworkMockTestSupport.isCdpActive(mock));
        assertTrue(NetworkMockTestSupport.isAttached(mock));

        // Clearing should complete without exceptions
        mock.clear();
        assertFalse(NetworkMockTestSupport.isAttached(mock));
    }

    @Test
    public void noDriverActive_logsWarningAndDoesNotThrow() {
        // No session override pushed, DriverManager.getDriver() throws IllegalStateException
        NetworkMock mock = NetworkMock.get();
        mock.stub("**/api/users").returnJson("[]");

        assertTrue(NetworkMockTestSupport.isAttached(mock));
        assertFalse(NetworkMockTestSupport.isBiDiActive(mock));
        assertFalse(NetworkMockTestSupport.isCdpActive(mock));

        mock.clear();
        assertFalse(NetworkMockTestSupport.isAttached(mock));
    }

    @Test
    public void sameStubTest_passesOnChromeAndFirefox() {
        // 1. Run stub logic on Firefox (BiDi)
        WebDriver firefoxDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(firefoxDriver);
        Network mockBidi = mock(Network.class);
        when(mockBidi.addIntercept(any())).thenReturn("bidi-fx-1");
        NetworkMockTestSupport.setBiDiFactory(d -> mockBidi);

        NetworkMock fxMock = NetworkMock.get();
        fxMock.stub("**/api/profile").returnJson("{\"username\":\"tester\"}");

        assertTrue(NetworkMockTestSupport.isBiDiActive(fxMock), "Firefox should activate BiDi interception");
        fxMock.clear();
        DriverManager.popSessionOverride();
        NetworkMock.cleanup();
        NetworkMockTestSupport.resetBiDiFactory();

        // 2. Run the exact same stub logic on Chrome (CDP fallback when BiDi fails or is unconfigured)
        ChromiumDriver chromeDriver = mock(ChromiumDriver.class);
        DevTools chromeDevTools = mock(DevTools.class);
        when(chromeDriver.getDevTools()).thenReturn(chromeDevTools);
        DriverManager.pushSessionOverride(chromeDriver);
        NetworkMockTestSupport.setBiDiFactory(d -> {
            throw new BiDiException("WebSocket connection not enabled on Chrome session");
        });

        NetworkMock chromeMock = NetworkMock.get();
        chromeMock.stub("**/api/profile").returnJson("{\"username\":\"tester\"}");

        assertTrue(NetworkMockTestSupport.isCdpActive(chromeMock), "Chrome should activate CDP interception when BiDi is absent");
        chromeMock.clear();
        DriverManager.popSessionOverride();
    }

    @Test
    public void cleanup_removesThreadLocalInstance() {
        WebDriver mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(HasBiDi.class));
        DriverManager.pushSessionOverride(mockDriver);

        Network mockNetwork = mock(Network.class);
        NetworkMockTestSupport.setBiDiFactory(d -> mockNetwork);

        NetworkMock first = NetworkMock.get();
        first.stub("**/api/users").returnJson("[]");

        NetworkMock.cleanup();

        NetworkMock second = NetworkMock.get();
        assertNotSame(first, second, "cleanup() should remove per-thread instance");
    }
}
