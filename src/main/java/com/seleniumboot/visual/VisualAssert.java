package com.seleniumboot.visual;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.driver.DriverManager;
import com.seleniumboot.internal.SeleniumBootContext;
import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Screenshot-based visual regression assertions.
 *
 * <p>On the first run, a baseline image is captured and saved automatically — the test passes.
 * On subsequent runs, the current screenshot is compared pixel-by-pixel against the baseline.
 * If the difference exceeds the configured threshold the test fails and a diff image
 * (changed pixels highlighted in red) is embedded in the HTML report.
 *
 * <p>To update baselines run with {@code -Dvisual.update=true}.
 *
 * <pre>{@code
 * // In a test
 * open("/dashboard");
 * VisualAssert.matchesBaseline("dashboard-main");
 * }</pre>
 *
 * <p>Configuration (selenium-boot.yml):
 * <pre>
 * visual:
 *   enabled: true
 *   diffThreshold: 0.02   # 2% — proportion of pixels allowed to differ
 * </pre>
 */
public final class VisualAssert {

    private static final String BASELINE_DIR = "src/test/resources/baselines";
    private static final String DIFF_OUTPUT_DIR = "target/reports/visual-diff";
    private static final double DEFAULT_THRESHOLD = 0.02;

    private VisualAssert() {
    }

    /**
     * Asserts that the current page screenshot matches the stored baseline named {@code name}.
     *
     * <ul>
     *   <li>First run — baseline does not exist: saves the screenshot as the new baseline,
     *       logs an INFO step, test continues without failure.</li>
     *   <li>{@code -Dvisual.update=true} — always overwrites the existing baseline and passes.</li>
     *   <li>Subsequent runs — compares current screenshot to baseline pixel-by-pixel.
     *       Fails if the diff ratio exceeds the configured threshold.</li>
     * </ul>
     *
     * @param name logical name for this checkpoint, used as the file name (e.g. "dashboard-header")
     */
    public static void matchesBaseline(String name) {
        if (!isEnabled()) {
            StepLogger.step("[VisualAssert] Skipped — visual.enabled is false", StepStatus.INFO);
            return;
        }

        byte[] currentBytes = captureCurrentScreenshot();
        if (currentBytes == null) {
            StepLogger.step("[VisualAssert] Could not capture screenshot for '" + name + "'", StepStatus.WARN);
            return;
        }

        Path baselinePath = Paths.get(BASELINE_DIR, name + ".png");
        boolean forceUpdate = Boolean.getBoolean("visual.update");

        // First run or forced update — save baseline and pass
        if (!Files.exists(baselinePath) || forceUpdate) {
            saveBaseline(baselinePath, currentBytes, name, forceUpdate);
            return;
        }

        // Compare against existing baseline
        compareAndAssert(name, baselinePath, currentBytes);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void saveBaseline(Path baselinePath, byte[] imageBytes, String name, boolean isUpdate) {
        try {
            Files.createDirectories(baselinePath.getParent());
            Files.write(baselinePath, imageBytes);
            String action = isUpdate ? "updated" : "created";
            StepLogger.step("[VisualAssert] Baseline " + action + " for '" + name + "' → " + baselinePath, StepStatus.INFO);
        } catch (IOException e) {
            StepLogger.step("[VisualAssert] Failed to save baseline for '" + name + "': " + e.getMessage(), StepStatus.WARN);
        }
    }

    private static void compareAndAssert(String name, Path baselinePath, byte[] currentBytes) {
        BufferedImage baseline;
        BufferedImage current;

        try {
            baseline = ImageIO.read(baselinePath.toFile());
            current  = ImageIO.read(new ByteArrayInputStream(currentBytes));
        } catch (IOException e) {
            StepLogger.step("[VisualAssert] Could not read images for '" + name + "': " + e.getMessage(), StepStatus.WARN);
            return;
        }

        if (baseline == null || current == null) {
            StepLogger.step("[VisualAssert] Invalid image data for '" + name + "'", StepStatus.WARN);
            return;
        }

        // Size mismatch — treat as a full diff immediately
        if (baseline.getWidth() != current.getWidth() || baseline.getHeight() != current.getHeight()) {
            String msg = String.format(
                "[VisualAssert] FAIL '%s' — size mismatch: baseline=%dx%d, current=%dx%d",
                name, baseline.getWidth(), baseline.getHeight(), current.getWidth(), current.getHeight()
            );
            StepLogger.step(msg, StepStatus.FAIL);
            throw new AssertionError(msg);
        }

        int width  = baseline.getWidth();
        int height = baseline.getHeight();
        long totalPixels = (long) width * height;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        long diffPixels = buildDiffImage(baseline, current, diffImage, width, height);

        double diffRatio = (double) diffPixels / totalPixels;
        double threshold = getThreshold();

        String diffPercent = String.format("%.2f%%", diffRatio * 100);
        String threshPercent = String.format("%.2f%%", threshold * 100);

        if (diffRatio <= threshold) {
            StepLogger.step(
                "[VisualAssert] PASS '" + name + "' — diff " + diffPercent + " within threshold " + threshPercent,
                StepStatus.PASS
            );
            return;
        }

        // Diff exceeds threshold — save diff image and fail
        String diffBase64 = saveDiffImage(name, diffImage);
        String failMsg = String.format(
            "[VisualAssert] FAIL '%s' — diff %s exceeds threshold %s (%d/%d pixels changed)",
            name, diffPercent, threshPercent, diffPixels, totalPixels
        );

        if (diffBase64 != null) {
            // Embed diff image into step timeline
            StepLogger.stepWithScreenshot(failMsg, StepStatus.FAIL, diffBase64);
        } else {
            StepLogger.step(failMsg, StepStatus.FAIL);
        }

        throw new AssertionError(failMsg);
    }

    /**
     * Builds a diff image where changed pixels are highlighted in red and
     * unchanged pixels show the baseline colour.
     *
     * @return number of pixels that differ between baseline and current
     */
    private static long buildDiffImage(BufferedImage baseline, BufferedImage current,
                                       BufferedImage diff, int width, int height) {
        long diffCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baselinePixel = baseline.getRGB(x, y);
                int currentPixel  = current.getRGB(x, y);
                if (baselinePixel != currentPixel) {
                    diff.setRGB(x, y, 0xFFFF0000); // red highlight
                    diffCount++;
                } else {
                    diff.setRGB(x, y, baselinePixel);
                }
            }
        }
        return diffCount;
    }

    /**
     * Saves the diff image to {@code target/reports/visual-diff/} and returns
     * its base64 encoding for report embedding. Returns {@code null} on failure.
     */
    private static String saveDiffImage(String name, BufferedImage diffImage) {
        try {
            Path diffDir = Paths.get(DIFF_OUTPUT_DIR);
            Files.createDirectories(diffDir);
            Path diffPath = diffDir.resolve(name + "-diff-" + System.currentTimeMillis() + ".png");
            ImageIO.write(diffImage, "PNG", diffPath.toFile());

            // Also return as base64 for embedding in the step timeline
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(diffImage, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            System.err.println("[VisualAssert] Could not save diff image: " + e.getMessage());
            return null;
        }
    }

    private static byte[] captureCurrentScreenshot() {
        WebDriver driver = DriverManager.getDriver();
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.err.println("[VisualAssert] Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }

    private static boolean isEnabled() {
        try {
            SeleniumBootConfig.Visual visual = SeleniumBootContext.getConfig().getVisual();
            return visual == null || visual.isEnabled();
        } catch (Exception e) {
            return true; // default enabled
        }
    }

    private static double getThreshold() {
        try {
            SeleniumBootConfig.Visual visual = SeleniumBootContext.getConfig().getVisual();
            if (visual != null) {
                return visual.getDiffThreshold();
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_THRESHOLD;
    }
}
