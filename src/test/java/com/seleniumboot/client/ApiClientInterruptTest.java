package com.seleniumboot.client;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.testng.Assert.*;

/** Verifies interrupt flag restoration in {@link ApiAuth} and {@link ApiClient}. */
public class ApiClientInterruptTest {

    @BeforeMethod
    @AfterMethod
    public void cleanup() {
        Thread.interrupted();
        ApiAuth.OAuth2TokenCache.clearCache();
    }

    @Test
    public void oauth2_whenInterrupted_restoresInterruptFlag() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:1/api"));
        try {
            Thread.currentThread().interrupt();
            RuntimeException ex = expectThrows(RuntimeException.class,
                    () -> ApiAuth.oauth2("http://127.0.0.1:1/token", "id", "secret").apply(builder));
            assertTrue(ex.getCause() instanceof InterruptedException,
                    "expected InterruptedException cause, got: " + ex.getCause());
            assertTrue(Thread.interrupted(), "interrupt flag should be restored after InterruptedException");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void send_whenInterrupted_restoresInterruptFlag() {
        try {
            Thread.currentThread().interrupt();
            RuntimeException ex = expectThrows(RuntimeException.class,
                    () -> ApiClient.get("http://127.0.0.1:1/health").send());
            assertTrue(ex.getCause() instanceof InterruptedException,
                    "expected InterruptedException cause, got: " + ex.getCause());
            assertTrue(Thread.interrupted(), "interrupt flag should be restored after InterruptedException");
        } finally {
            Thread.interrupted();
        }
    }
}
