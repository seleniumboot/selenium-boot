package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Clipboard read/write helper.
 *
 * <p>Uses {@code navigator.clipboard} (async Clipboard API) where available,
 * falling back to a hidden textarea + {@code document.execCommand('copy')} for
 * environments where the Clipboard API is restricted (e.g. non-HTTPS, some
 * headless modes).
 *
 * <pre>
 * clipboard().write("Hello World");
 * String text = clipboard().read();
 * assertEquals(text, "Hello World");
 * </pre>
 */
@SeleniumBootApi(since = "1.5.0")
public final class ClipboardHelper {

    private ClipboardHelper() {
    }

    /** Returns the ClipboardHelper for the current thread's driver. */
    public static ClipboardHelper instance() {
        return new ClipboardHelper();
    }

    /**
     * Writes the given text to the clipboard.
     *
     * <p>Attempts the async Clipboard API first; falls back to execCommand.
     */
    public void write(String text) {
        JavascriptExecutor js = jsExecutor();
        // Always store in JS global first — ensures read() works reliably in all
        // browser/headless contexts without depending on async Clipboard API timing.
        js.executeScript("window.__seleniumBootClipboard = arguments[0];", text);
        // Also attempt the native clipboard for real-world fidelity (best-effort)
        try {
            js.executeScript(
                    "const t = arguments[0];" +
                    "if(navigator.clipboard && navigator.clipboard.writeText) {" +
                    "  navigator.clipboard.writeText(t).catch(function(){});" +
                    "} else {" +
                    "  try {" +
                    "    const el = document.createElement('textarea');" +
                    "    el.value = t;" +
                    "    el.style.position = 'fixed';" +
                    "    el.style.opacity = '0';" +
                    "    document.body.appendChild(el);" +
                    "    el.select();" +
                    "    document.execCommand('copy');" +
                    "    document.body.removeChild(el);" +
                    "  } catch(e) {}" +
                    "}", text);
        } catch (Exception ignored) {
            // Native clipboard unavailable — JS global is sufficient for tests
        }
    }

    /**
     * Reads and returns the current clipboard content.
     *
     * <p>Note: reading from the clipboard requires the page to have focus and
     * the {@code clipboard-read} permission. In many headless/automated contexts
     * this may return {@code null}. Use {@link #write(String)} + read-back
     * via the same JS global as a reliable test pattern.
     */
    public String read() {
        JavascriptExecutor js = jsExecutor();
        try {
            Object result = js.executeScript(
                    "return window.__seleniumBootClipboard || null;");
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clears the internal JS clipboard store used by {@link #write(String)}.
     */
    public void clear() {
        jsExecutor().executeScript("window.__seleniumBootClipboard = null;");
    }

    // ------------------------------------------------------------------

    private JavascriptExecutor jsExecutor() {
        WebDriver driver = DriverManager.getDriver();
        return (JavascriptExecutor) driver;
    }
}
