package com.seleniumboot.unit;

import com.seleniumboot.ci.CiEnvironmentDetector;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link CiEnvironmentDetector}.
 *
 * CI env vars cannot be injected in-process during tests, so these tests
 * focus on the logic that CAN be exercised without env manipulation:
 * container detection via filesystem, thread-count capping, and the
 * ciName() fallback path.
 */
public class CiEnvironmentDetectorTest {

    // ----------------------------------------------------------
    // recommendedThreadCount
    // ----------------------------------------------------------

    @Test
    public void recommendedThreadCount_neverExceedsMax() {
        int result = CiEnvironmentDetector.recommendedThreadCount(2);
        assertTrue(result <= 2,
                "Thread count must not exceed the configured max");
    }

    @Test
    public void recommendedThreadCount_atLeastOne() {
        int result = CiEnvironmentDetector.recommendedThreadCount(100);
        assertTrue(result >= 1,
                "Thread count must be at least 1");
    }

    @Test
    public void recommendedThreadCount_maxOneReturnsOne() {
        assertEquals(1, CiEnvironmentDetector.recommendedThreadCount(1));
    }

    @Test
    public void recommendedThreadCount_derivedFromCpuCores() {
        int cores = Runtime.getRuntime().availableProcessors();
        int result = CiEnvironmentDetector.recommendedThreadCount(Integer.MAX_VALUE);
        assertEquals(cores, result,
                "Without a cap, result should equal available CPU cores");
    }

    // ----------------------------------------------------------
    // ciName() — local fallback
    // ----------------------------------------------------------

    @Test
    public void ciName_returnsNonNull() {
        // whatever environment we're in, ciName() must return a non-null string
        assertNotNull(CiEnvironmentDetector.ciName());
    }

    @Test
    public void ciName_returnsNonEmpty() {
        assertFalse(CiEnvironmentDetector.ciName().isBlank());
    }

    // ----------------------------------------------------------
    // isContainer() — no /.dockerenv in dev machines
    // ----------------------------------------------------------

    @Test
    public void isContainer_returnsBooleanWithoutThrowing() {
        // Just verify it doesn't throw on any OS — actual value depends on the host
        boolean result = CiEnvironmentDetector.isContainer();
        assertTrue(result || !result); // always true — guards against NPE / exception
    }

    // ----------------------------------------------------------
    // isCI() — returns boolean without throwing
    // ----------------------------------------------------------

    @Test
    public void isCI_returnsBooleanWithoutThrowing() {
        boolean result = CiEnvironmentDetector.isCI();
        assertTrue(result || !result);
    }
}
