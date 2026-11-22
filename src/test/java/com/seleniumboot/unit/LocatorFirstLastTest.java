package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import com.seleniumboot.locator.Role;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorFirstLastTest {


    private WebDriver mockDriver;
    private MockedStatic<DriverManager> driverManagerMock;
    private MockedStatic<SeleniumBootContext> contextMock;

    @BeforeMethod
    public void setup() {
        mockDriver = mock(WebDriver.class);

        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(DriverManager::getDriver).thenReturn(mockDriver);

        // Supply a config so the auto-wait in element() can read timeouts.explicit
        SeleniumBootConfig.Timeouts timeouts = new SeleniumBootConfig.Timeouts();
        timeouts.setExplicit(2);
        SeleniumBootConfig config = new SeleniumBootConfig();
        config.setTimeouts(timeouts);

        contextMock = mockStatic(SeleniumBootContext.class);
        contextMock.when(SeleniumBootContext::getConfig).thenReturn(config);
    }

    @AfterMethod
    public void teardown() {
        driverManagerMock.close();
        contextMock.close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // first() & last()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void first_returnsFirstMatchedElement() {
        WebElement el1 = mock(WebElement.class);
        when(el1.isDisplayed()).thenReturn(true);
        when(el1.getText()).thenReturn("Item 1");

        WebElement el2 = mock(WebElement.class);
        when(el2.isDisplayed()).thenReturn(true);
        when(el2.getText()).thenReturn("Item 2");

        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(Arrays.asList(el1, el2));

        String text = Locator.ofCss(".item").first().getText();
        assertEquals(text, "Item 1");
    }

    @Test
    public void last_returnsLastMatchedElement() {
        WebElement el1 = mock(WebElement.class);
        when(el1.isDisplayed()).thenReturn(true);
        when(el1.getText()).thenReturn("Item 1");

        WebElement el2 = mock(WebElement.class);
        when(el2.isDisplayed()).thenReturn(true);
        when(el2.getText()).thenReturn("Item 2");

        WebElement el3 = mock(WebElement.class);
        when(el3.isDisplayed()).thenReturn(true);
        when(el3.getText()).thenReturn("Item 3");

        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(Arrays.asList(el1, el2, el3));

        String text = Locator.ofCss(".item").last().getText();
        assertEquals(text, "Item 3");
    }

    @Test
    public void first_and_last_onSingleElement_returnThatElement() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        when(el.getText()).thenReturn("Only Item");

        when(mockDriver.findElements(By.cssSelector(".single"))).thenReturn(Collections.singletonList(el));

        assertEquals(Locator.ofCss(".single").first().getText(), "Only Item");
        assertEquals(Locator.ofCss(".single").last().getText(), "Only Item");
    }

    @Test
    public void last_resolvesLazilyAtActionTime() {
        // last() should NOT query the driver at build time
        Locator locator = Locator.ofCss(".item").last();
        verifyNoInteractions(mockDriver);

        WebElement el1 = mock(WebElement.class);
        when(el1.isDisplayed()).thenReturn(true);
        when(el1.getText()).thenReturn("First");

        WebElement el2 = mock(WebElement.class);
        when(el2.isDisplayed()).thenReturn(true);
        when(el2.getText()).thenReturn("Second");

        WebElement el3 = mock(WebElement.class);
        when(el3.isDisplayed()).thenReturn(true);
        when(el3.getText()).thenReturn("Third");

        // Action 1: 2 items in DOM
        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(Arrays.asList(el1, el2));
        assertEquals(locator.getText(), "Second");

        // Action 2: 3 items in DOM now — last() dynamically gets the 3rd item
        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(Arrays.asList(el1, el2, el3));
        assertEquals(locator.getText(), "Third");
    }

    @Test(expectedExceptions = LocatorException.class)
    public void first_throwsLocatorException_whenNoElementsMatch() {
        when(mockDriver.findElements(By.cssSelector(".missing"))).thenReturn(Collections.emptyList());

        Locator.ofCss(".missing").first().element();
    }

    @Test(expectedExceptions = LocatorException.class)
    public void last_throwsLocatorException_whenNoElementsMatch() {
        when(mockDriver.findElements(By.cssSelector(".missing"))).thenReturn(Collections.emptyList());

        Locator.ofCss(".missing").last().element();
    }

    @Test
    public void first_and_last_exceptionMessage_identifiesRequestedNarrowing() {
        when(mockDriver.findElements(By.cssSelector(".missing"))).thenReturn(Collections.emptyList());

        try {
            Locator.ofCss(".missing").first().element();
            fail("Expected LocatorException");
        } catch (LocatorException e) {
            assertTrue(e.getMessage().contains("first() requested"), "Message should mention first()");
            assertTrue(e.getMessage().contains(".first()"), "Description should include .first()");
        }

        try {
            Locator.ofCss(".missing").last().element();
            fail("Expected LocatorException");
        } catch (LocatorException e) {
            assertTrue(e.getMessage().contains("last() requested"), "Message should mention last()");
            assertTrue(e.getMessage().contains(".last()"), "Description should include .last()");
        }
    }

    @Test
    public void first_and_last_toString_formatting() {
        Locator firstLoc = Locator.ofCss(".item").first();
        assertTrue(firstLoc.toString().contains(".first()"), "toString should include .first()");
        assertFalse(firstLoc.toString().contains(".nth("), "toString should not include .nth(0)");

        Locator lastLoc = Locator.ofCss(".item").last();
        assertTrue(lastLoc.toString().contains(".last()"), "toString should include .last()");
        assertFalse(lastLoc.toString().contains(".nth("), "toString should not include .nth");

        Locator roleFirst = Locator.byRole(Role.LISTITEM).first();
        assertTrue(roleFirst.toString().contains("getByRole(listitem).first()"));

        Locator roleLast = Locator.byRole(Role.LISTITEM).last();
        assertTrue(roleLast.toString().contains("getByRole(listitem).last()"));
    }

    @Test
    public void first_last_nth_overridePrecedence() {
        Locator loc1 = Locator.ofCss(".item").first().last();
        assertTrue(loc1.toString().contains(".last()"));
        assertFalse(loc1.toString().contains(".first()"));

        Locator loc2 = Locator.ofCss(".item").last().first();
        assertTrue(loc2.toString().contains(".first()"));
        assertFalse(loc2.toString().contains(".last()"));

        Locator loc3 = Locator.ofCss(".item").nth(3).first();
        assertTrue(loc3.toString().contains(".first()"));
        assertFalse(loc3.toString().contains(".nth(3)"));

        Locator loc4 = Locator.ofCss(".item").first().nth(2);
        assertTrue(loc4.toString().contains(".nth(2)"));
        assertFalse(loc4.toString().contains(".first()"));

        Locator loc5 = Locator.ofCss(".item").last().nth(2);
        assertTrue(loc5.toString().contains(".nth(2)"));
        assertFalse(loc5.toString().contains(".last()"));
    }
}
