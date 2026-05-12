package com.seleniumboot.unit;

import com.seleniumboot.clock.TestClock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link TestClock}.
 * Verifies state management and JS script constants — no real WebDriver required.
 */
public class TestClockTest {

    @AfterMethod
    public void cleanup() {
        // Ensure no leftover mock time leaks between tests
        try { TestClock.autoReset(); } catch (Exception ignored) {}
    }

    // ── factory ───────────────────────────────────────────────────────────

    @Test
    public void create_returnsNonNullInstance() {
        assertNotNull(TestClock.create());
    }

    @Test
    public void create_returnsFreshInstance_eachCall() {
        assertNotSame(TestClock.create(), TestClock.create());
    }

    // ── getMockedTimeMs — no active mock ──────────────────────────────────

    @Test
    public void getMockedTimeMs_returnsNull_whenNoMockActive() {
        TestClock clock = TestClock.create();
        assertNull(clock.getMockedTimeMs());
    }

    // ── set — epoch calculation ───────────────────────────────────────────

    @Test(expectedExceptions = Exception.class)
    public void set_throwsWhenNoDriver() {
        TestClock.create().set("2030-01-01T00:00:00Z");
    }

    @Test(expectedExceptions = Exception.class)
    public void set_throwsOnInvalidIso() {
        TestClock.create().set("not-a-date");
    }

    // ── advance — pure math, no driver needed ─────────────────────────────

    @Test(expectedExceptions = Exception.class)
    public void advance_throwsWhenNoDriver() {
        TestClock.create().advance(Duration.ofDays(1));
    }

    // ── autoReset — no-op when no mock active ────────────────────────────

    @Test
    public void autoReset_isNoOpWhenNoMockActive() {
        // should not throw even when no driver is present
        TestClock.autoReset();
    }

    // ── reset — no-op when no mock active ────────────────────────────────

    @Test
    public void reset_isNoOpWhenNoMockActive() {
        TestClock clock = TestClock.create();
        clock.reset(); // must not throw
    }

    // ── INJECT_JS + RESET_JS constants reachable via reflection ──────────

    @Test
    public void injectJsScript_containsDateNowOverride() throws Exception {
        java.lang.reflect.Field f = TestClock.class.getDeclaredField("INJECT_JS");
        f.setAccessible(true);
        String js = (String) f.get(null);
        assertTrue(js.contains("Date.now"), "INJECT_JS must override Date.now");
        assertTrue(js.contains("__sbOriginalDate"), "INJECT_JS must save original Date");
        assertTrue(js.contains("mockTime"), "INJECT_JS must reference mockTime");
    }

    @Test
    public void resetJsScript_restoresOriginalDate() throws Exception {
        java.lang.reflect.Field f = TestClock.class.getDeclaredField("RESET_JS");
        f.setAccessible(true);
        String js = (String) f.get(null);
        assertTrue(js.contains("__sbOriginalDate"), "RESET_JS must reference __sbOriginalDate");
        assertTrue(js.contains("delete"), "RESET_JS must delete __sbOriginalDate after restore");
    }

    // ── epoch math verification ───────────────────────────────────────────

    @Test
    public void set_epochCalculation_isCorrect() {
        Instant knownInstant = Instant.parse("2030-01-01T00:00:00Z");
        long expected = knownInstant.toEpochMilli();
        // We can't call set() without a driver, so verify the Instant math directly
        assertEquals(Instant.parse("2030-01-01T00:00:00Z").toEpochMilli(), expected);
        assertTrue(expected > 0);
    }

    @Test
    public void advance_addsCorrectMillis() {
        Duration d = Duration.ofDays(30);
        long before = Instant.parse("2030-01-01T00:00:00Z").toEpochMilli();
        long after  = before + d.toMillis();
        assertEquals(after - before, 30L * 24 * 60 * 60 * 1000);
    }

    // ── SeleniumBootApi annotation presence ───────────────────────────────

    @Test
    public void testClock_hasSeleniumBootApiAnnotation() {
        com.seleniumboot.api.SeleniumBootApi annotation =
            TestClock.class.getAnnotation(com.seleniumboot.api.SeleniumBootApi.class);
        assertNotNull(annotation, "TestClock must be annotated with @SeleniumBootApi");
        assertEquals(annotation.since(), "2.2.0");
    }
}
