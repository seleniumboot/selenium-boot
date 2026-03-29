package com.seleniumboot.recording;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Captures a screen recording during test execution.
 *
 * <p>A background thread takes screenshots at a configurable FPS.
 * On test pass, frames are discarded. On test failure, frames are
 * assembled into an animated GIF saved to {@code target/recordings/}.
 *
 * <p>ThreadLocal-based — safe for parallel test execution.
 */
public final class RecordingManager {

    private static final ThreadLocal<RecordingSession> SESSION = new ThreadLocal<>();

    private RecordingManager() {}

    /**
     * Starts a screen recording session for the current thread.
     *
     * @param driver             the WebDriver instance to screenshot
     * @param fps                frames per second (1–10 recommended)
     * @param maxDurationSeconds hard cap on recording length
     */
    public static void start(WebDriver driver, int fps, int maxDurationSeconds) {
        stop(); // discard any leftover session from a previous test
        if (!(driver instanceof TakesScreenshot)) return;

        int  maxFrames  = Math.max(1, fps) * Math.max(1, maxDurationSeconds);
        long intervalMs = 1000L / Math.max(1, fps);

        RecordingSession session = new RecordingSession((TakesScreenshot) driver, maxFrames, fps);
        SESSION.set(session);
        session.start(intervalMs);
    }

    /**
     * Stops recording and discards all captured frames (called on test success).
     */
    public static void stop() {
        RecordingSession session = SESSION.get();
        if (session != null) {
            session.cancel();
            SESSION.remove();
        }
    }

    /**
     * Stops recording and saves captured frames as an animated GIF.
     * Called on test failure.
     *
     * @param testId the fully-qualified test method name (used as filename)
     * @return the path to the saved GIF, or {@code null} if saving failed or no frames were captured
     */
    public static String saveOnFailure(String testId) {
        RecordingSession session = SESSION.get();
        if (session == null) return null;
        session.cancel();
        SESSION.remove();

        List<BufferedImage> frames = session.getFrames();
        if (frames.isEmpty()) return null;

        String safeId = testId.replaceAll("[^a-zA-Z0-9._-]", "_");
        File   dir    = new File("target/recordings");
        dir.mkdirs();
        File output = new File(dir, safeId + ".gif");

        int delayMs = 1000 / Math.max(1, session.getFps());
        try {
            GifEncoder.write(frames, output, delayMs);
            return output.getPath();
        } catch (IOException e) {
            System.err.println("[Selenium Boot] Failed to save recording for '" + testId + "': " + e.getMessage());
            return null;
        }
    }

    // ── Inner class ──────────────────────────────────────────────────────────

    private static final class RecordingSession {

        private final TakesScreenshot         driver;
        private final int                     maxFrames;
        private final int                     fps;
        private final List<BufferedImage>     frames   = new CopyOnWriteArrayList<>();
        private       ScheduledExecutorService executor;
        private       ScheduledFuture<?>       future;

        RecordingSession(TakesScreenshot driver, int maxFrames, int fps) {
            this.driver    = driver;
            this.maxFrames = maxFrames;
            this.fps       = fps;
        }

        void start(long intervalMs) {
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "selenium-boot-recorder");
                t.setDaemon(true);
                return t;
            });
            future = executor.scheduleAtFixedRate(this::capture, 0, intervalMs, TimeUnit.MILLISECONDS);
        }

        private void capture() {
            if (frames.size() >= maxFrames) {
                cancel();
                return;
            }
            try {
                byte[]      png = driver.getScreenshotAs(OutputType.BYTES);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
                if (img != null) frames.add(img);
            } catch (Exception ignored) {
                // Driver may be in the middle of navigation or closing; silently skip
            }
        }

        void cancel() {
            if (future   != null) future.cancel(false);
            if (executor != null) executor.shutdownNow();
        }

        List<BufferedImage> getFrames() { return new ArrayList<>(frames); }
        int getFps()                     { return fps; }
    }
}
