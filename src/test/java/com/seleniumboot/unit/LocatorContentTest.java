package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import org.openqa.selenium.OutputType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorContentTest {


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

    @Test
    public void allTextContents_returnsAllMatchesText() {
        WebElement el1 = mock(WebElement.class);
        when(el1.getText()).thenReturn(" Alpha ");
        WebElement el2 = mock(WebElement.class);
        when(el2.getText()).thenReturn("Beta");
        WebElement el3 = mock(WebElement.class);
        when(el3.getText()).thenReturn(" Gamma");

        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(List.of(el1, el2, el3));

        List<String> texts = Locator.ofCss(".item").allTextContents();

        assertEquals(texts, List.of("Alpha", "Beta", "Gamma"));
    }

    @Test
    public void allTextContents_fallsBackToDomPropertyWhenGetTextEmpty() {
        WebElement el1 = mock(WebElement.class);
        when(el1.getText()).thenReturn("");
        when(el1.getDomProperty("textContent")).thenReturn("Hidden Alpha");

        when(mockDriver.findElements(By.cssSelector(".item"))).thenReturn(List.of(el1));

        List<String> texts = Locator.ofCss(".item").allTextContents();

        assertEquals(texts, List.of("Hidden Alpha"));
    }

    @Test
    public void allTextContents_returnsEmptyListWhenNoMatches_noWait() {
        when(mockDriver.findElements(By.cssSelector(".missing"))).thenReturn(Collections.emptyList());

        List<String> texts = Locator.ofCss(".missing").allTextContents();

        assertNotNull(texts);
        assertTrue(texts.isEmpty());
    }

    @Test
    public void screenshot_returnsPngBytes() {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        byte[] expectedBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        when(el.getScreenshotAs(OutputType.BYTES)).thenReturn(expectedBytes);

        when(mockDriver.findElements(By.id("card"))).thenReturn(List.of(el));

        byte[] bytes = Locator.of(By.id("card")).screenshot();

        assertEquals(bytes, expectedBytes);
        verify(el, times(1)).getScreenshotAs(OutputType.BYTES);
    }

    @Test
    public void screenshot_writesToPath() throws IOException {
        WebElement el = mock(WebElement.class);
        when(el.isDisplayed()).thenReturn(true);
        byte[] expectedBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x01, 0x02};
        when(el.getScreenshotAs(OutputType.BYTES)).thenReturn(expectedBytes);

        when(mockDriver.findElements(By.id("card"))).thenReturn(List.of(el));

        Path tempDir = Files.createTempDirectory("locator-screenshot-test");
        Path subDir = tempDir.resolve("nested");
        Path targetFile = subDir.resolve("element.png");
        try {
            Locator.of(By.id("card")).screenshot(targetFile);

            assertTrue(Files.exists(targetFile));
            assertEquals(Files.readAllBytes(targetFile), expectedBytes);
        } finally {
            Files.deleteIfExists(targetFile);
            Files.deleteIfExists(subDir);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void screenshot_nullPath_throwsIllegalArgumentException() {
        Locator.of(By.id("card")).screenshot(null);
    }
}
