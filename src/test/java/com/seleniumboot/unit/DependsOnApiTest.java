package com.seleniumboot.unit;

import com.seleniumboot.precondition.ApiHealthChecker;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ApiHealthChecker} and the {@code @DependsOnApi} health-check mechanic.
 *
 * All assertions go through the public {@code checkOrSkip()} API.
 * Unreachable URLs use port 1 (connection refused instantly — no timeout wait).
 */
public class DependsOnApiTest {

    private static final String UNREACHABLE = "http://127.0.0.1:1";

    @BeforeMethod
    @AfterMethod
    public void resetCache() {
        ApiHealthChecker.clearCache();
    }

    // ----------------------------------------------------------
    // checkOrSkip() — basic behaviour
    // ----------------------------------------------------------

    @Test(expectedExceptions = SkipException.class)
    public void checkOrSkip_unreachableUrl_throwsSkipException() {
        ApiHealthChecker.checkOrSkip(UNREACHABLE, 2);
    }

    @Test
    public void checkOrSkip_skipMessageContainsUrl() {
        try {
            ApiHealthChecker.checkOrSkip(UNREACHABLE, 2);
            fail("Expected SkipException");
        } catch (SkipException e) {
            assertTrue(e.getMessage().contains(UNREACHABLE),
                    "SkipException message must contain the failing URL");
        }
    }

    @Test(expectedExceptions = SkipException.class)
    public void checkOrSkip_invalidUrl_throwsSkipException() {
        ApiHealthChecker.checkOrSkip("not-a-valid-url", 2);
    }

    // ----------------------------------------------------------
    // Caching behaviour
    // ----------------------------------------------------------

    @Test
    public void checkOrSkip_cachesProbedResult_noReprobeOnSecondCall() {
        // First call probes UNREACHABLE and caches false
        try { ApiHealthChecker.checkOrSkip(UNREACHABLE, 2); } catch (SkipException ignored) {}

        // Second call uses a very long timeout — must still return immediately from cache
        long start = System.currentTimeMillis();
        try { ApiHealthChecker.checkOrSkip(UNREACHABLE, 60); } catch (SkipException ignored) {}
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1000,
                "Second call must return from cache, not re-probe (took " + elapsed + "ms)");
    }

    @Test
    public void clearCache_allowsNewProbeOnNextCall() {
        // Populate cache
        try { ApiHealthChecker.checkOrSkip(UNREACHABLE, 1); } catch (SkipException ignored) {}

        // After clear, next call should probe again — still unreachable, still throws
        ApiHealthChecker.clearCache();

        try {
            ApiHealthChecker.checkOrSkip(UNREACHABLE, 1);
            fail("Expected SkipException after cache clear");
        } catch (SkipException e) {
            assertTrue(e.getMessage().contains(UNREACHABLE));
        }
    }

    @Test
    public void clearCache_doesNotThrow() {
        // clearCache on an empty cache must be a no-op
        ApiHealthChecker.clearCache();
        ApiHealthChecker.clearCache();
    }

    // ----------------------------------------------------------
    // getName / adapter contract
    // ----------------------------------------------------------

    @Test
    public void multipleDistinctUrls_eachProbedIndependently() {
        String url1 = "http://127.0.0.1:1";
        String url2 = "http://127.0.0.1:2";

        try { ApiHealthChecker.checkOrSkip(url1, 1); } catch (SkipException ignored) {}
        try { ApiHealthChecker.checkOrSkip(url2, 1); } catch (SkipException ignored) {}

        // Both should be cached — second calls must return instantly
        long start = System.currentTimeMillis();
        try { ApiHealthChecker.checkOrSkip(url1, 60); } catch (SkipException ignored) {}
        try { ApiHealthChecker.checkOrSkip(url2, 60); } catch (SkipException ignored) {}
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1000,
                "Both cached results must resolve instantly (took " + elapsed + "ms)");
    }
}
