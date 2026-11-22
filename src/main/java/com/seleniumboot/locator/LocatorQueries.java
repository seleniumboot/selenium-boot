package com.seleniumboot.locator;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Bodies of {@link Locator}'s querying terminal actions (getText, count, rows, screenshot, …).
 * The public methods on {@link Locator} stay in place and delegate here, so the public API
 * is unchanged. Package-private implementation detail of the locator package.
 */
final class LocatorQueries {

    private LocatorQueries() {
    }

    static String getText(Locator l) {
        return LocatorWaits.whenVisible(l, el -> el.getText().trim());
    }

    static String getAttribute(Locator l, String name) {
        return LocatorWaits.whenVisible(l, el -> el.getAttribute(name));
    }

    static String inputValue(Locator l) {
        return LocatorWaits.whenVisible(l, el -> {
            String val = el.getDomProperty("value");
            if (val == null) {
                val = el.getAttribute("value");
            }
            return val != null ? val : "";
        });
    }

    static boolean isVisible(Locator l) {
        try {
            return LocatorResolution.resolve(l).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isHidden(Locator l) {
        return !isVisible(l);
    }

    static boolean isEnabled(Locator l) {
        try {
            return LocatorResolution.resolve(l).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isChecked(Locator l) {
        try {
            return Locator.isSelected(LocatorResolution.resolve(l));
        } catch (Exception e) {
            return false;
        }
    }

    static int count(Locator l) {
        return LocatorResolution.resolveAll(l).size();
    }

    static List<String> allTextContents(Locator l) {
        SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
        int timeout = cfg.getTimeouts().getExplicit();
        AtomicReference<List<String>> result = new AtomicReference<>();
        new WebDriverWait(l.driver(), Duration.ofSeconds(timeout))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    try {
                        List<WebElement> candidates = LocatorResolution.resolveAll(l);
                        if (candidates.isEmpty()) {
                            result.set(Collections.emptyList());
                            return Boolean.TRUE;
                        }
                        List<String> texts = new ArrayList<>(candidates.size());
                        for (WebElement el : candidates) {
                            String text = el.getText();
                            if (text == null || text.isBlank()) {
                                String dom = el.getDomProperty("textContent");
                                if (dom != null && !dom.isBlank()) {
                                    text = dom;
                                }
                            }
                            texts.add(text != null ? text.trim() : "");
                        }
                        result.set(texts);
                        return Boolean.TRUE;
                    } catch (LocatorException e) {
                        result.set(Collections.emptyList());
                        return Boolean.TRUE;
                    }
                });
        return result.get();
    }

    static WebElement element(Locator l) {
        return LocatorWaits.whenVisible(l, el -> el);
    }

    static List<WebElement> elements(Locator l) {
        return LocatorResolution.resolveAll(l).stream()
                .filter(WebElement::isDisplayed)
                .collect(Collectors.toList());
    }

    static List<Map<String, String>> rows(Locator l) {
        WebElement table = element(l);

        List<WebElement> allRows = table.findElements(By.cssSelector("tr"));

        // Empty table
        if (allRows.isEmpty()) {
            return new ArrayList<>();
        }

        WebElement headerRow = null;
        List<WebElement> headerCells = new ArrayList<>();

        // 1. Try to find header cells inside <thead>
        List<WebElement> theadRows = table.findElements(By.cssSelector("thead tr"));

        if (!theadRows.isEmpty()) {
            List<WebElement> theadHeaders =
                    theadRows.get(0).findElements(By.cssSelector("th"));

            if (!theadHeaders.isEmpty()) {
                headerRow = theadRows.get(0);
                headerCells = theadHeaders;
            }
        }

        // 2. No <thead> header -> find the first <tr> containing <th>
        if (headerCells.isEmpty()) {
            for (WebElement row : allRows) {
                List<WebElement> thCells =
                        row.findElements(By.cssSelector("th"));

                if (!thCells.isEmpty()) {
                    headerRow = row;
                    headerCells = thCells;
                    break;
                }
            }
        }

        // 3. No <th> at all -> use the first <tr> as the header
        if (headerCells.isEmpty()) {
            headerRow = allRows.get(0);
            headerCells = headerRow.findElements(By.cssSelector("td"));
        }

        // Convert header WebElements into Strings
        List<String> headers = headerCells.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .collect(Collectors.toList());

        if (headers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, String>> result = new ArrayList<>();

        // Convert each data row into a Map
        for (WebElement row : allRows) {

            // Do not treat the header row as data
            if (row.equals(headerRow)) {
                continue;
            }

            List<WebElement> cells =
                    row.findElements(By.cssSelector("td"));

            if (cells.isEmpty()) {
                continue;
            }

            Map<String, String> rowMap =
                    new LinkedHashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);

                String cellText = i < cells.size()
                        ? cells.get(i).getText().trim()
                        : "";

                rowMap.put(header, cellText);
            }

            result.add(rowMap);
        }

        return result;
    }

    static byte[] screenshot(Locator l) {
        return LocatorWaits.whenVisible(l, el -> el.getScreenshotAs(OutputType.BYTES));
    }

    static void screenshot(Locator l, Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Screenshot path cannot be null");
        }
        byte[] bytes = screenshot(l);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new LocatorException("Failed to write screenshot to '" + path + "': " + e.getMessage(), e);
        }
    }
}
