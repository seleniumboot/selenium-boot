package com.seleniumboot.unit;

import com.seleniumboot.shadow.ShadowDom;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class ShadowDomTest {

    // ── find(WebElement, String) ──────────────────────────────────────────

    @Test
    public void find_returnsElementFromShadowRoot() {
        WebElement host   = Mockito.mock(WebElement.class);
        SearchContext root = Mockito.mock(SearchContext.class);
        WebElement target  = Mockito.mock(WebElement.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElement(By.cssSelector("#btn"))).thenReturn(target);

        WebElement result = ShadowDom.find(host, "#btn");
        assertSame(result, target);
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void find_throwsWhenSelectorMatchesNothing() {
        WebElement host   = Mockito.mock(WebElement.class);
        SearchContext root = Mockito.mock(SearchContext.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElement(By.cssSelector(".missing")))
               .thenThrow(new NoSuchElementException("not found"));

        ShadowDom.find(host, ".missing");
    }

    // ── findAll(WebElement, String) ───────────────────────────────────────

    @Test
    public void findAll_returnsAllMatchingElements() {
        WebElement host    = Mockito.mock(WebElement.class);
        SearchContext root  = Mockito.mock(SearchContext.class);
        WebElement item1   = Mockito.mock(WebElement.class);
        WebElement item2   = Mockito.mock(WebElement.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElements(By.cssSelector(".item")))
               .thenReturn(Arrays.asList(item1, item2));

        List<WebElement> result = ShadowDom.findAll(host, ".item");
        assertEquals(result.size(), 2);
    }

    @Test
    public void findAll_returnsEmptyListWhenNoMatch() {
        WebElement host   = Mockito.mock(WebElement.class);
        SearchContext root = Mockito.mock(SearchContext.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElements(By.cssSelector(".none")))
               .thenReturn(Collections.emptyList());

        List<WebElement> result = ShadowDom.findAll(host, ".none");
        assertTrue(result.isEmpty());
    }

    // ── exists(WebElement, String) ────────────────────────────────────────

    @Test
    public void exists_returnsTrueWhenElementFound() {
        WebElement host   = Mockito.mock(WebElement.class);
        SearchContext root = Mockito.mock(SearchContext.class);
        WebElement el     = Mockito.mock(WebElement.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElements(By.cssSelector(".badge")))
               .thenReturn(Collections.singletonList(el));

        assertTrue(ShadowDom.exists(host, ".badge"));
    }

    @Test
    public void exists_returnsFalseWhenNothingFound() {
        WebElement host   = Mockito.mock(WebElement.class);
        SearchContext root = Mockito.mock(SearchContext.class);

        Mockito.when(host.getShadowRoot()).thenReturn(root);
        Mockito.when(root.findElements(By.cssSelector(".gone")))
               .thenReturn(Collections.emptyList());

        assertFalse(ShadowDom.exists(host, ".gone"));
    }

    @Test
    public void exists_returnsFalseOnException() {
        WebElement host = Mockito.mock(WebElement.class);
        Mockito.when(host.getShadowRoot()).thenThrow(new RuntimeException("no shadow root"));

        assertFalse(ShadowDom.exists(host, ".anything"));
    }

    // ── pierce (JS) ───────────────────────────────────────────────────────

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void pierce_requiresAtLeastTwoSelectors() {
        // Argument guard runs before the driver call — no driver mock needed
        ShadowDom.pierce("only-one");
    }

    @Test
    public void buildPierceJs_containsAllSelectors() {
        String js = ShadowDom.buildPierceJs("outer-host", "inner-host", "#target");
        assertTrue(js.contains("outer-host"),  "JS should contain first selector");
        assertTrue(js.contains("inner-host"),  "JS should contain intermediate selector");
        assertTrue(js.contains("#target"),      "JS should contain final selector");
        assertTrue(js.contains("shadowRoot"),   "JS should traverse shadowRoot");
    }

    @Test
    public void buildPierceJs_escapesSpecialCharacters() {
        String js = ShadowDom.buildPierceJs("host[data-id='x']", ".target");
        assertTrue(js.contains("\\'"), "Single quotes inside selectors should be escaped");
    }

    @Test
    public void buildPierceJs_twoSelectorsProducesValidStructure() {
        String js = ShadowDom.buildPierceJs("my-host", "#button");
        assertTrue(js.startsWith("(function()"),   "Should be an IIFE");
        assertTrue(js.contains("document.querySelector"), "Should start from document");
        assertTrue(js.endsWith(")()"),              "IIFE should be immediately invoked");
    }
}
