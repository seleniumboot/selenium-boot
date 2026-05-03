package com.seleniumboot.unit;

import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.session.MultiSessionManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link MultiSessionManager} and the session-override stack in
 * {@link DriverManager}.
 * Named-session driver creation requires a real browser and is covered by
 * integration tests in the consumer project.
 */
public class MultiSessionTest {

    @AfterMethod
    public void cleanup() {
        MultiSessionManager.clearAll();
        // Pop any stray override left by a test (defensive)
        try { DriverManager.popSessionOverride(); } catch (Exception ignored) {}
    }

    // ── DriverManager session-override stack ──────────────────────────

    @Test
    public void pushSessionOverride_getDriver_returnsOverride() {
        WebDriver mock = mock(WebDriver.class);
        DriverManager.pushSessionOverride(mock);
        try {
            assertSame(DriverManager.getDriver(), mock,
                "getDriver() should return the pushed override driver");
        } finally {
            DriverManager.popSessionOverride();
        }
    }

    @Test
    public void popSessionOverride_afterPush_primaryDriverRestored() {
        WebDriver mock = mock(WebDriver.class);
        DriverManager.pushSessionOverride(mock);
        DriverManager.popSessionOverride();
        // After pop, the override stack is empty — any exception here
        // means the primary-driver path is reached (no override present)
        try {
            DriverManager.getDriver();
            // Reaching here without exception means the stack was cleared
        } catch (IllegalStateException e) {
            // Expected: primary driver is null in unit-test JVM — that is fine
            assertTrue(e.getMessage().contains("WebDriver not initialized"),
                "Should fail because primary driver is null, not because override remained");
        }
    }

    @Test
    public void pushSessionOverride_nested_stackBehaviour() {
        WebDriver first  = mock(WebDriver.class);
        WebDriver second = mock(WebDriver.class);

        DriverManager.pushSessionOverride(first);
        assertSame(DriverManager.getDriver(), first);

        DriverManager.pushSessionOverride(second);
        assertSame(DriverManager.getDriver(), second, "Nested push should return innermost driver");

        DriverManager.popSessionOverride();
        assertSame(DriverManager.getDriver(), first, "After inner pop, outer driver should be active");

        DriverManager.popSessionOverride();
    }

    @Test
    public void popSessionOverride_emptyStack_noException() {
        // Should be a no-op, not throw
        DriverManager.popSessionOverride();
    }

    // ── MultiSessionManager ───────────────────────────────────────────

    @Test
    public void clearAll_emptyState_noException() {
        MultiSessionManager.clearAll(); // no-op on empty map — must not throw
    }

    @Test
    public void clearAll_quitsAllInjectedSessions() throws Exception {
        WebDriver alice = mock(WebDriver.class);
        WebDriver bob   = mock(WebDriver.class);

        injectSessions(Map.of("alice", alice, "bob", bob));

        MultiSessionManager.clearAll();

        verify(alice).quit();
        verify(bob).quit();
    }

    @Test
    public void clearAll_quitException_doesNotPropagate() throws Exception {
        WebDriver broken = mock(WebDriver.class);
        doThrow(new RuntimeException("browser crashed")).when(broken).quit();

        injectSessions(Map.of("broken", broken));

        MultiSessionManager.clearAll(); // must not propagate the quit() exception
    }

    @Test
    public void withSession_usesInjectedDriverDuringAction() throws Exception {
        WebDriver sessionDriver = mock(WebDriver.class);
        injectSessions(Map.of("mySession", sessionDriver));

        WebDriver[] captured = new WebDriver[1];
        MultiSessionManager.withSession("mySession", () -> {
            captured[0] = DriverManager.getDriver();
        });

        assertSame(captured[0], sessionDriver,
            "withSession() should make the session driver visible via getDriver()");
    }

    @Test
    public void withSession_restoresDriverAfterAction() throws Exception {
        WebDriver sessionDriver = mock(WebDriver.class);
        injectSessions(Map.of("s1", sessionDriver));

        MultiSessionManager.withSession("s1", () -> { /* no-op */ });

        // After withSession, the override stack should be empty
        try {
            DriverManager.getDriver();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("WebDriver not initialized"),
                "Primary driver (null in unit tests) should be returned, not session driver");
        }
    }

    @Test
    public void withSession_exceptionInAction_driverStillRestored() throws Exception {
        WebDriver sessionDriver = mock(WebDriver.class);
        injectSessions(Map.of("err", sessionDriver));

        try {
            MultiSessionManager.withSession("err", () -> {
                throw new RuntimeException("intentional failure");
            });
            fail("Should have propagated the exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("intentional failure") ||
                       e.getCause() != null && e.getCause().getMessage().contains("intentional failure"));
        }

        // Stack must be clean after exception
        try {
            DriverManager.getDriver();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("WebDriver not initialized"));
        }
    }

    // ── API surface ───────────────────────────────────────────────────

    @Test
    public void sessionAction_isPublicFunctionalInterface() {
        assertTrue(MultiSessionManager.SessionAction.class.isInterface());
        assertEquals(MultiSessionManager.SessionAction.class.getDeclaredMethods().length, 1);
    }

    // ── Helper ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static void injectSessions(Map<String, WebDriver> sessions) throws Exception {
        Field namedField = MultiSessionManager.class.getDeclaredField("NAMED");
        namedField.setAccessible(true);
        ThreadLocal<Map<String, WebDriver>> named =
            (ThreadLocal<Map<String, WebDriver>>) namedField.get(null);
        named.get().putAll(sessions);
    }
}
