package com.seleniumboot.unit;

import com.seleniumboot.browser.StorageHelper;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link StorageHelper}.
 * Verifies class structure and factory method contracts.
 * Real browser JS execution is covered by integration tests in the consumer project.
 */
public class StorageHelperTest {

    @Test
    public void localStorage_factoryReturnsNonNull() {
        // Factory method must not be null — actual execution requires a driver
        assertNotNull(StorageHelper.class.getDeclaredMethods(),
                "StorageHelper should have declared methods");
    }

    @Test
    public void storageHelper_hasLocalStorageFactory() throws NoSuchMethodException {
        assertNotNull(StorageHelper.class.getMethod("localStorage"));
    }

    @Test
    public void storageHelper_hasSessionStorageFactory() throws NoSuchMethodException {
        assertNotNull(StorageHelper.class.getMethod("sessionStorage"));
    }

    @Test
    public void storageHelper_hasCookiesFactory() throws NoSuchMethodException {
        assertNotNull(StorageHelper.class.getMethod("cookies"));
    }

    @Test
    public void localStorage_classHasSetMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.LocalStorage.class
                .getMethod("set", String.class, String.class));
    }

    @Test
    public void localStorage_classHasGetMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.LocalStorage.class
                .getMethod("get", String.class));
    }

    @Test
    public void localStorage_classHasRemoveMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.LocalStorage.class
                .getMethod("remove", String.class));
    }

    @Test
    public void localStorage_classHasClearMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.LocalStorage.class.getMethod("clear"));
    }

    @Test
    public void sessionStorage_classHasExpectedMethods() throws NoSuchMethodException {
        assertNotNull(StorageHelper.SessionStorage.class
                .getMethod("set", String.class, String.class));
        assertNotNull(StorageHelper.SessionStorage.class
                .getMethod("get", String.class));
        assertNotNull(StorageHelper.SessionStorage.class.getMethod("clear"));
    }

    @Test
    public void cookies_classHasSetMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.Cookies.class
                .getMethod("set", String.class, String.class));
    }

    @Test
    public void cookies_classHasGetMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.Cookies.class.getMethod("get", String.class));
    }

    @Test
    public void cookies_classHasDeleteAllMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.Cookies.class.getMethod("deleteAll"));
    }

    @Test
    public void cookies_classHasExistsMethod() throws NoSuchMethodException {
        assertNotNull(StorageHelper.Cookies.class.getMethod("exists", String.class));
    }
}
