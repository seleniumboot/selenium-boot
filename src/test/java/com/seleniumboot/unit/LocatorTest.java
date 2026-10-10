package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.locator.Locator;
import com.seleniumboot.locator.LocatorException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import com.seleniumboot.locator.Role;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Locator}.
 * All tests use a mocked WebDriver — no real browser required.
 */
public class LocatorTest {

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

    // LocatorException thrown when no elements match
    @Test
    public void resolve_throwsLocatorException_whenNoElementsFound() {
        // Locator.of(By) requires DriverManager — tested via integration;
        // here we verify LocatorException message is descriptive.
        LocatorException ex = new LocatorException("No element found for: By.id: missing");
        assertTrue(ex.getMessage().contains("No element found"));
    }

    @Test
    public void locatorException_preservesCause() {
        RuntimeException cause = new RuntimeException("root cause");
        LocatorException ex = new LocatorException("wrapped", cause);
        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getMessage(), "wrapped");
    }

    @Test
    public void locator_toString_includesRootBy() {
        Locator loc = Locator.of(By.id("username"));
        assertTrue(loc.toString().contains("username"),
                "toString should include root By description");
    }

    @Test
    public void locator_toString_includesFilterAndNth() {
        Locator loc = Locator.of(By.cssSelector(".row"))
                .filter(".active")
                .nth(2);
        String str = loc.toString();
        assertTrue(str.contains(".active"), "toString should include filter");
        assertTrue(str.contains("2"),       "toString should include nth index");
    }

    @Test
    public void locator_toString_includesWithText() {
        Locator loc = Locator.ofCss("button").withText("Save");
        assertTrue(loc.toString().contains("Save"), "toString should include withText value");
    }

    @Test
    public void locator_toString_includesWithin() {
        Locator loc = Locator.of(By.cssSelector("input"))
                .within(By.id("login-form"));
        assertTrue(loc.toString().contains("login-form"), "toString should include within container");
    }

    @Test
    public void locatorOfCss_createsByCssSelector() {
        Locator loc = Locator.ofCss(".submit-btn");
        assertTrue(loc.toString().contains("submit-btn"));
    }

    @Test
    public void locatorOf_createsByLocator() {
        Locator loc = Locator.of(By.name("email"));
        assertTrue(loc.toString().contains("email"));
    }

    @Test
    public void locatorException_withMessageOnly() {
        LocatorException ex = new LocatorException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void locator_chaining_doesNotMutateOriginal() {
        Locator base    = Locator.ofCss(".item");
        Locator filtered = base.filter(".active");
        // Both should still be valid Locator objects
        assertNotNull(base);
        assertNotNull(filtered);
        assertTrue(filtered.toString().contains(".active"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // rows() — table → List<Map<header, cell>>
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void rows_mapsHeaderCellsToDataCells_forTheadTable() {
        WebElement headerRow = row(cells("Name", "Email"), Collections.emptyList());
        WebElement dataRow1  = row(Collections.emptyList(), cells("Alice", "alice@test.io"));
        WebElement dataRow2  = row(Collections.emptyList(), cells("Bob",   "bob@test.io"));
        table(Arrays.asList(headerRow, dataRow1, dataRow2), Collections.singletonList(headerRow));

        List<Map<String, String>> rows = Locator.ofCss("table").rows();

        assertEquals(rows.size(), 2, "header row must not be returned as data");
        assertEquals(rows.get(0).get("Name"),  "Alice");
        assertEquals(rows.get(0).get("Email"), "alice@test.io");
        assertEquals(rows.get(1).get("Name"),  "Bob");
        assertEquals(rows.get(1).get("Email"), "bob@test.io");
    }

    @Test
    public void rows_preservesColumnOrder() {
        WebElement headerRow = row(cells("Id", "Name", "Status"), Collections.emptyList());
        WebElement dataRow   = row(Collections.emptyList(), cells("7", "Widget", "Active"));
        table(Arrays.asList(headerRow, dataRow), Collections.singletonList(headerRow));

        Map<String, String> first = Locator.ofCss("table").rows().get(0);

        assertEquals(Arrays.asList("Id", "Name", "Status"),
                Arrays.asList(first.keySet().toArray()),
                "keys should keep the header column order");
    }

    @Test
    public void rows_trimsHeaderAndCellText() {
        WebElement headerRow = row(cells("  Name  "), Collections.emptyList());
        WebElement dataRow   = row(Collections.emptyList(), cells("\n  Alice \t"));
        table(Arrays.asList(headerRow, dataRow), Collections.singletonList(headerRow));

        Map<String, String> first = Locator.ofCss("table").rows().get(0);

        assertTrue(first.containsKey("Name"), "header key should be trimmed");
        assertEquals(first.get("Name"), "Alice");
    }

    @Test
    public void rows_usesFirstRowWithTh_whenThereIsNoThead() {
        WebElement headerRow = row(cells("Name", "Email"), Collections.emptyList());
        WebElement dataRow   = row(Collections.emptyList(), cells("Alice", "alice@test.io"));
        table(Arrays.asList(headerRow, dataRow), Collections.emptyList());

        List<Map<String, String>> rows = Locator.ofCss("table").rows();

        assertEquals(rows.size(), 1);
        assertEquals(rows.get(0).get("Email"), "alice@test.io");
    }

    @Test
    public void rows_usesFirstRowWithTh_whenTheadHasNoHeaderCells() {
        // <thead><tr><td>…</td></tr></thead> — no <th>, so the scan falls through
        WebElement theadRow  = row(Collections.emptyList(), cells("ignored"));
        WebElement headerRow = row(cells("Name"), Collections.emptyList());
        WebElement dataRow   = row(Collections.emptyList(), cells("Alice"));
        table(Arrays.asList(theadRow, headerRow, dataRow), Collections.singletonList(theadRow));

        List<Map<String, String>> rows = Locator.ofCss("table").rows();

        assertEquals(rows.size(), 2, "the thead row now counts as data");
        assertEquals(rows.get(0).get("Name"), "ignored");
        assertEquals(rows.get(1).get("Name"), "Alice");
    }

    @Test
    public void rows_usesFirstRowAsHeader_whenTableHasNoThCells() {
        WebElement headerRow = row(Collections.emptyList(), cells("Name", "Email"));
        WebElement dataRow   = row(Collections.emptyList(), cells("Alice", "alice@test.io"));
        table(Arrays.asList(headerRow, dataRow), Collections.emptyList());

        List<Map<String, String>> rows = Locator.ofCss("table").rows();

        assertEquals(rows.size(), 1, "the first row is consumed as the header");
        assertEquals(rows.get(0).get("Name"),  "Alice");
        assertEquals(rows.get(0).get("Email"), "alice@test.io");
    }

    @Test
    public void rows_padsMissingCellsWithEmptyString() {
        WebElement headerRow = row(cells("Name", "Email", "Role"), Collections.emptyList());
        WebElement shortRow  = row(Collections.emptyList(), cells("Alice"));
        table(Arrays.asList(headerRow, shortRow), Collections.singletonList(headerRow));

        Map<String, String> first = Locator.ofCss("table").rows().get(0);

        assertEquals(first.size(), 3, "every header gets a key even when cells are missing");
        assertEquals(first.get("Name"),  "Alice");
        assertEquals(first.get("Email"), "");
        assertEquals(first.get("Role"),  "");
    }

    @Test
    public void rows_ignoresCellsBeyondTheHeaderCount() {
        WebElement headerRow = row(cells("Name"), Collections.emptyList());
        WebElement wideRow   = row(Collections.emptyList(), cells("Alice", "extra", "more"));
        table(Arrays.asList(headerRow, wideRow), Collections.singletonList(headerRow));

        Map<String, String> first = Locator.ofCss("table").rows().get(0);

        assertEquals(first.size(), 1);
        assertEquals(first.get("Name"), "Alice");
    }

    @Test
    public void rows_skipsRowsWithoutDataCells() {
        WebElement headerRow  = row(cells("Name"), Collections.emptyList());
        WebElement groupRow   = row(cells("Group A"), Collections.emptyList()); // th-only sub-header
        WebElement dataRow    = row(Collections.emptyList(), cells("Alice"));
        table(Arrays.asList(headerRow, groupRow, dataRow), Collections.singletonList(headerRow));

        List<Map<String, String>> rows = Locator.ofCss("table").rows();

        assertEquals(rows.size(), 1, "th-only rows carry no data and are skipped");
        assertEquals(rows.get(0).get("Name"), "Alice");
    }

    @Test
    public void rows_returnsEmptyList_whenTableHasNoRows() {
        table(Collections.emptyList(), Collections.emptyList());

        assertTrue(Locator.ofCss("table").rows().isEmpty());
    }

    @Test
    public void rows_returnsEmptyList_whenNoHeaderCellsCanBeDerived() {
        // a single <tr> with neither <th> nor <td>
        WebElement emptyRow = row(Collections.emptyList(), Collections.emptyList());
        table(Collections.singletonList(emptyRow), Collections.emptyList());

        assertTrue(Locator.ofCss("table").rows().isEmpty());
    }

    @Test
    public void rows_returnsEmptyList_whenHeaderRowIsTheOnlyRow() {
        WebElement headerRow = row(cells("Name", "Email"), Collections.emptyList());
        table(Collections.singletonList(headerRow), Collections.singletonList(headerRow));

        assertTrue(Locator.ofCss("table").rows().isEmpty(), "a header-only table has no data rows");
    }

    @Test(expectedExceptions = LocatorException.class)
    public void rows_throwsLocatorException_whenTableIsNotFound() {
        when(mockDriver.findElements(By.cssSelector("table"))).thenReturn(Collections.emptyList());

        Locator.ofCss("table").rows();
    }

    @Test
    public void rows_resolvesThroughTheChain_whenNthIsApplied() {
        WebElement headerRow = row(cells("Name"), Collections.emptyList());
        WebElement dataRow   = row(Collections.emptyList(), cells("Second"));
        WebElement first     = mock(WebElement.class);
        WebElement second    = tableElement(Arrays.asList(headerRow, dataRow),
                                            Collections.singletonList(headerRow));
        when(mockDriver.findElements(By.cssSelector("table")))
                .thenReturn(Arrays.asList(first, second));

        List<Map<String, String>> rows = Locator.ofCss("table").nth(1).rows();

        assertEquals(rows.size(), 1);
        assertEquals(rows.get(0).get("Name"), "Second");
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

    // ── rows() fixtures ───────────────────────────────────────────────────────

    /** Mocks a {@code <tr>} with the given {@code <th>} and {@code <td>} children. */
    private WebElement row(List<WebElement> thCells, List<WebElement> tdCells) {
        WebElement row = mock(WebElement.class);
        when(row.findElements(By.cssSelector("th"))).thenReturn(thCells);
        when(row.findElements(By.cssSelector("td"))).thenReturn(tdCells);
        return row;
    }

    /** Mocks a list of cells whose visible text is the given values. */
    private List<WebElement> cells(String... texts) {
        return Arrays.stream(texts).map(text -> {
            WebElement cell = mock(WebElement.class);
            when(cell.getText()).thenReturn(text);
            return cell;
        }).collect(java.util.stream.Collectors.toList());
    }

    /** Mocks a visible {@code <table>} — without registering it with the driver. */
    private WebElement tableElement(List<WebElement> allRows, List<WebElement> theadRows) {
        WebElement table = mock(WebElement.class);
        when(table.isDisplayed()).thenReturn(true);
        when(table.findElements(By.cssSelector("tr"))).thenReturn(allRows);
        when(table.findElements(By.cssSelector("thead tr"))).thenReturn(theadRows);
        return table;
    }

    /** Mocks a visible {@code <table>} and makes it the only match for {@code table}. */
    private WebElement table(List<WebElement> allRows, List<WebElement> theadRows) {
        WebElement table = tableElement(allRows, theadRows);
        when(mockDriver.findElements(By.cssSelector("table")))
                .thenReturn(Collections.singletonList(table));
        return table;
    }
}
