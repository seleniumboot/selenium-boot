package com.seleniumboot.precondition;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Verifies interrupt flag restoration in {@link ApiHealthChecker}. */
public class ApiHealthCheckerInterruptTest {

    @BeforeMethod
    @AfterMethod
    public void cleanup() {
        Thread.interrupted();
        ApiHealthChecker.clearCache();
    }

    @Test
    public void probe_whenInterrupted_restoresInterruptFlag() {
        try {
            Thread.currentThread().interrupt();
            boolean up = ApiHealthChecker.probe("http://127.0.0.1:1", 2);
            assertFalse(up);
            assertTrue(Thread.interrupted(), "interrupt flag should be restored after InterruptedException");
        } finally {
            Thread.interrupted();
        }
    }
}
