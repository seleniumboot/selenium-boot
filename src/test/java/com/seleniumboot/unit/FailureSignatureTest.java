package com.seleniumboot.unit;

import com.seleniumboot.failure.FailureSignature;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Signature normalisation: same cause groups together, different cause does not. */
public class FailureSignatureTest {

    private static String trace(String type, String msg, String frame) {
        return type + ": " + msg + "\n"
                + "\tat org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:300)\n"
                + "\tat " + frame + "(Page.java:42)\n"
                + "\tat com.example.tests.LoginTest.run(LoginTest.java:10)\n";
    }

    private static String sig(String type, String msg, String frame) {
        return FailureSignature.of(msg, trace(type, msg, frame));
    }

    @Test
    public void sameCause_differentNumbersAndIds_sameSignature() {
        String a = sig("org.openqa.selenium.NoSuchElementException",
                "no such element: #order-1042 after 5000ms (session 3f2a9c1e-1111-2222-3333-444455556666)",
                "com.example.pages.OrdersPage.open");
        String b = sig("org.openqa.selenium.NoSuchElementException",
                "no such element: #order-77 after 12000ms (session aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee)",
                "com.example.pages.OrdersPage.open");
        Assert.assertEquals(a, b);
    }

    @Test
    public void quotedValuesAreIgnored() {
        Assert.assertEquals(
                sig("java.lang.AssertionError", "expected 'Alice' but found 'Bob'", "com.example.A.m"),
                sig("java.lang.AssertionError", "expected 'Carol' but found 'Dan'", "com.example.A.m"));
    }

    @Test
    public void differentMessage_differentSignature() {
        Assert.assertNotEquals(
                sig("org.openqa.selenium.NoSuchElementException", "no such element: login button", "com.example.A.m"),
                sig("org.openqa.selenium.NoSuchElementException", "no such element: logout link", "com.example.A.m"));
    }

    @Test
    public void differentCategory_differentSignature() {
        Assert.assertNotEquals(
                sig("org.openqa.selenium.StaleElementReferenceException", "boom", "com.example.A.m"),
                sig("org.openqa.selenium.ElementNotInteractableException", "boom", "com.example.A.m"));
    }

    @Test
    public void differentApplicationFrame_differentSignature() {
        Assert.assertNotEquals(
                sig("org.openqa.selenium.NoSuchElementException", "gone", "com.example.pages.LoginPage.submit"),
                sig("org.openqa.selenium.NoSuchElementException", "gone", "com.example.pages.CartPage.submit"));
    }

    @Test
    public void lineNumberShift_doesNotSplitGroup() {
        String a = FailureSignature.of("gone", "org.openqa.selenium.NoSuchElementException: gone\n"
                + "\tat com.example.pages.LoginPage.submit(LoginPage.java:10)\n");
        String b = FailureSignature.of("gone", "org.openqa.selenium.NoSuchElementException: gone\n"
                + "\tat com.example.pages.LoginPage.submit(LoginPage.java:99)\n");
        Assert.assertEquals(a, b);
    }

    @Test
    public void frameworkFramesAreSkipped() {
        String s = FailureSignature.of("gone", "org.openqa.selenium.NoSuchElementException: gone\n"
                + "\tat com.seleniumboot.locator.Locator.click(Locator.java:1)\n"
                + "\tat com.example.pages.LoginPage.submit(LoginPage.java:10)\n");
        Assert.assertTrue(s.endsWith("| com.example.pages.LoginPage.submit"), s);
    }

    @Test
    public void noData_doesNotThrow() {
        Assert.assertNotNull(FailureSignature.of(null, null));
    }
}
