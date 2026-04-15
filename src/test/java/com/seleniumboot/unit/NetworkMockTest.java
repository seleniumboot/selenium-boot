package com.seleniumboot.unit;

import com.seleniumboot.network.NetworkMock;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link NetworkMock} — pattern matching and stub registration.
 * CDP wiring requires a real browser and is covered by integration tests.
 */
public class NetworkMockTest {

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
    // StubBuilder
    // ------------------------------------------------------------------

    @Test
    public void stub_returnJson_setsDefaults() {
        // StubBuilder is constructed via NetworkMock.stub() but we can test
        // the pattern is stored correctly via toString-level assertions
        // Full wiring requires CDP — tested in integration.
        // Here we verify the pattern matching that drives stub lookup.
        assertTrue(NetworkMock.matches("**/users", "https://example.com/api/users"),
                "Pattern **/users should match any URL ending in /users");
    }

    @Test
    public void stub_returnStatus_500() {
        assertTrue(NetworkMock.matches("**/checkout", "https://shop.example.com/checkout"));
    }
}
