package com.seleniumboot.network;

import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Test-support helper for accessing package-private testing hooks in {@link NetworkMock}.
 * Lives exclusively in test scope ({@code src/test/java}) so it is never exposed in the
 * published framework artifact.
 */
public final class NetworkMockTestSupport {

    private NetworkMockTestSupport() {
    }

    @FunctionalInterface
    public interface BiDiNetworkFactory {
        org.openqa.selenium.bidi.module.Network create(WebDriver driver);
    }

    public static void setBiDiFactory(BiDiNetworkFactory factory) {
        NetworkMock.setBiDiFactory(factory::create);
    }

    public static void resetBiDiFactory() {
        NetworkMock.resetBiDiFactory();
    }

    public static boolean isAttached(NetworkMock mock) {
        return mock.isAttached();
    }

    public static boolean isBiDiActive(NetworkMock mock) {
        return mock.isBiDiActive();
    }

    public static boolean isCdpActive(NetworkMock mock) {
        return mock.isCdpActive();
    }

    public static List<StubBuilder> getStubs(NetworkMock mock) {
        return mock.getStubs();
    }
}

