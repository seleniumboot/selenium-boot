package com.seleniumboot.visual;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Visual regression assertion using pixel-by-pixel comparison.
 *
 * <p><b>First run</b> — no baseline exists: screenshot is saved as the baseline and
 * the test passes with a warning. Inspect the baseline before committing.
 *
 * <p><b>Subsequent runs</b> — baseline exists: current screenshot is compared against
 * the baseline. If the pixel difference exceeds the configured tolerance the test fails;
 * a diff image is saved to {@code target/visual-diffs/}.
 *
 * <p><b>Updating baselines</b> — run with {@code -DupdateBaselines=true} to force
 * all screenshots to overwrite the baseline (useful after intentional UI changes).
 *
 * <pre>
 * // Full-page comparison, exact match
 * assertScreenshot("homepage");
 *
 * // Full-page with 2% pixel tolerance
 * assertScreenshot("checkout", VisualTolerance.of(2));
 *
 * // Element-scoped comparison
 * assertScreenshot("login-form", By.id("login-form"));
 *
 * // Element-scoped with tolerance
 * assertScreenshot("login-form", By.id("login-form"), VisualTolerance.of(1));
 * </pre>
 */
@SeleniumBootApi(since = "1.6.0")
public final class VisualAssert {

    private static final Logger LOG = Logger.getLogger(VisualAssert.class.getName());

    /** System property that forces baseline regeneration. */
    private static final boolean UPDATE_BASELINES =
            Boolean.parseBoolean(System.getProperty("updateBaselines", "false"));

    private VisualAssert() {
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Full-page comparison with default tolerance (0%). */
    public static void assertScreenshot(String name) {
        assertScreenshot(name, (By) null, defaultTolerance());
    }

    /** Full-page comparison with custom tolerance. */
    public static void assertScreenshot(String name, VisualTolerance tolerance) {
        assertScreenshot(name, (By) null, tolerance);
    }

    /** Element-scoped comparison with default tolerance. */
    public static void assertScreenshot(String name, By region) {
        assertScreenshot(name, region, defaultTolerance());
    }

    /** Element-scoped comparison with custom tolerance. */
    public static void assertScreenshot(String name, By region, VisualTolerance tolerance) {
        WebDriver driver = DriverManager.getDriver();
        BufferedImage current = capture(driver, region);
        compare(name, current, tolerance);
    }

    // ------------------------------------------------------------------
    // Core logic
    // ------------------------------------------------------------------

    public static void compare(String name, BufferedImage current, VisualTolerance tolerance) {
        File baseline = baselineFile(name);

        // Update mode — force overwrite baseline
        if (UPDATE_BASELINES) {
            saveBaseline(baseline, current);
            LOG.info("[VisualAssert] Baseline updated: " + baseline.getAbsolutePath());
            return;
        }

        // First run — no baseline yet
        if (!baseline.exists()) {
            saveBaseline(baseline, current);
            LOG.warning("[VisualAssert] No baseline found for '" + name +
                    "'. Saved as new baseline: " + baseline.getAbsolutePath() +
                    ". Review it before committing.");
            return;
        }

        // Load baseline
        BufferedImage base;
        try {
            base = ImageIO.read(baseline);
        } catch (IOException e) {
            Assert.fail("[VisualAssert] Failed to read baseline '" + name + "': " + e.getMessage());
            return;
        }

        // Resize current to match baseline dimensions if they differ (handles minor
        // viewport fluctuations — controlled by tolerance)
        if (current.getWidth() != base.getWidth() || current.getHeight() != base.getHeight()) {
            current = resize(current, base.getWidth(), base.getHeight());
        }

        // Pixel diff
        DiffResult result = diff(base, current);
        double diffPercent = result.diffPercent();

        if (diffPercent > tolerance.getPercent()) {
            // Save diff image
            File diffFile = diffFile(name);
            saveDiff(diffFile, result.diffImage());
            Assert.fail(String.format(
                    "[VisualAssert] '%s' differs by %.2f%% (tolerance: %.2f%%). " +
                    "Diff image: %s",
                    name, diffPercent, tolerance.getPercent(), diffFile.getAbsolutePath()));
        }
    }

    // ------------------------------------------------------------------
    // Screenshot capture
    // ------------------------------------------------------------------

    public static BufferedImage capture(WebDriver driver, By region) {
        byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        BufferedImage full;
        try {
            full = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException("[VisualAssert] Failed to read screenshot: " + e.getMessage(), e);
        }

        if (region == null) return full;

        // Crop to element bounds
        WebElement el = driver.findElement(region);
        org.openqa.selenium.Point loc  = el.getLocation();
        org.openqa.selenium.Dimension sz = el.getSize();

        int x = Math.max(0, loc.getX());
        int y = Math.max(0, loc.getY());
        int w = Math.min(sz.getWidth(),  full.getWidth()  - x);
        int h = Math.min(sz.getHeight(), full.getHeight() - y);

        if (w <= 0 || h <= 0) return full;
        return full.getSubimage(x, y, w, h);
    }

    // ------------------------------------------------------------------
    // Diff calculation
    // ------------------------------------------------------------------

    public static DiffResult diff(BufferedImage base, BufferedImage current) {
        int width  = base.getWidth();
        int height = base.getHeight();
        long diffPixels = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baseRgb    = base.getRGB(x, y);
                int currentRgb = current.getRGB(x, y);

                if (baseRgb != currentRgb) {
                    diffPixels++;
                    diffImage.setRGB(x, y, Color.RED.getRGB());
                } else {
                    // Dim matching pixels to make diffs stand out
                    int r = (baseRgb >> 16) & 0xFF;
                    int g = (baseRgb >>  8) & 0xFF;
                    int b =  baseRgb        & 0xFF;
                    diffImage.setRGB(x, y, new Color(r / 3, g / 3, b / 3).getRGB());
                }
            }
        }

        long totalPixels = (long) width * height;
        return new DiffResult(diffPixels, totalPixels, diffImage);
    }

    // ------------------------------------------------------------------
    // File helpers
    // ------------------------------------------------------------------

    private static File baselineFile(String name) {
        String dir = baselineDir();
        return new File(dir, sanitize(name) + ".png");
    }

    private static File diffFile(String name) {
        String dir = diffDir();
        new File(dir).mkdirs();
        return new File(dir, sanitize(name) + "-diff.png");
    }

    private static void saveBaseline(File file, BufferedImage image) {
        file.getParentFile().mkdirs();
        try {
            ImageIO.write(image, "PNG", file);
        } catch (IOException e) {
            throw new RuntimeException("[VisualAssert] Failed to save baseline: " + e.getMessage(), e);
        }
    }

    private static void saveDiff(File file, BufferedImage image) {
        try {
            ImageIO.write(image, "PNG", file);
        } catch (IOException e) {
            LOG.warning("[VisualAssert] Failed to save diff image: " + e.getMessage());
        }
    }

    private static BufferedImage resize(BufferedImage src, int targetW, int targetH) {
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static String baselineDir() {
        try {
            com.seleniumboot.config.SeleniumBootConfig config = SeleniumBootContext.getConfig();
            if (config.getVisual() != null && config.getVisual().getBaselineDir() != null) {
                return config.getVisual().getBaselineDir();
            }
        } catch (Exception ignored) {}
        return "src/test/resources/baselines";
    }

    private static String diffDir() {
        try {
            com.seleniumboot.config.SeleniumBootConfig config = SeleniumBootContext.getConfig();
            if (config.getVisual() != null && config.getVisual().getDiffDir() != null) {
                return config.getVisual().getDiffDir();
            }
        } catch (Exception ignored) {}
        return "target/visual-diffs";
    }

    private static VisualTolerance defaultTolerance() {
        try {
            com.seleniumboot.config.SeleniumBootConfig config = SeleniumBootContext.getConfig();
            if (config.getVisual() != null) {
                return VisualTolerance.of(config.getVisual().getDefaultTolerance());
            }
        } catch (Exception ignored) {}
        return VisualTolerance.exact();
    }

    // ------------------------------------------------------------------
    // Inner: DiffResult
    // ------------------------------------------------------------------

    public static final class DiffResult {
        private final long diffPixels;
        private final long totalPixels;
        private final BufferedImage diffImage;

        public DiffResult(long diffPixels, long totalPixels, BufferedImage diffImage) {
            this.diffPixels  = diffPixels;
            this.totalPixels = totalPixels;
            this.diffImage   = diffImage;
        }

        public double diffPercent() {
            if (totalPixels == 0) return 0;
            return (diffPixels * 100.0) / totalPixels;
        }

        public BufferedImage diffImage()  { return diffImage; }
        public long          diffPixels() { return diffPixels; }
        public long          totalPixels(){ return totalPixels; }
    }
}
