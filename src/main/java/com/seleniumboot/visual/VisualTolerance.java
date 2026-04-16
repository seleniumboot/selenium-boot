package com.seleniumboot.visual;

/**
 * Pixel-difference tolerance for {@link VisualAssert}.
 *
 * <pre>
 * assertScreenshot("home", VisualTolerance.of(2));  // 2% tolerance
 * assertScreenshot("home", VisualTolerance.exact()); // 0% — must be identical
 * </pre>
 */
public final class VisualTolerance {

    private final double percent;

    private VisualTolerance(double percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Tolerance must be between 0 and 100, got: " + percent);
        }
        this.percent = percent;
    }

    /** Exact match — zero pixel difference allowed. */
    public static VisualTolerance exact() {
        return new VisualTolerance(0);
    }

    /** Allow up to {@code percent}% of pixels to differ. */
    public static VisualTolerance of(double percent) {
        return new VisualTolerance(percent);
    }

    public double getPercent() {
        return percent;
    }
}
