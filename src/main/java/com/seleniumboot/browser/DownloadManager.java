package com.seleniumboot.browser;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.internal.SeleniumBootContext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for verifying file downloads during tests.
 *
 * <p>Configure the download directory in {@code selenium-boot.yml}:
 * <pre>
 * browser:
 *   downloadDir: ./target/downloads
 * </pre>
 *
 * <p>Usage:
 * <pre>
 * click(By.id("export-csv"));
 * DownloadManager.waitForFile("report.csv", 15);
 * DownloadManager.clearDownloads();
 * </pre>
 */
@SeleniumBootApi(since = "0.8.0")
public final class DownloadManager {

    private static final int POLL_INTERVAL_MS = 500;

    private DownloadManager() {}

    /**
     * Waits until a file with the given name appears in the download directory.
     *
     * @param filename       exact file name to wait for (e.g. {@code "report.csv"})
     * @param timeoutSeconds maximum seconds to wait
     * @return the matched {@link File}
     * @throws RuntimeException if the file does not appear within the timeout
     */
    public static File waitForFile(String filename, int timeoutSeconds) {
        File dir = resolveDownloadDir();
        long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;

        while (System.currentTimeMillis() < deadline) {
            File target = new File(dir, filename);
            if (target.exists() && target.length() > 0 && !isPartialDownload(target)) {
                return target;
            }
            sleep();
        }
        throw new RuntimeException(
            "[DownloadManager] File '" + filename + "' did not appear in " +
            dir.getAbsolutePath() + " within " + timeoutSeconds + " seconds."
        );
    }

    /**
     * Waits until any new file appears in the download directory.
     *
     * @param timeoutSeconds maximum seconds to wait
     * @return the first completed download file found
     * @throws RuntimeException if no file appears within the timeout
     */
    public static File waitForAnyFile(int timeoutSeconds) {
        File dir = resolveDownloadDir();
        long deadline = System.currentTimeMillis() + (long) timeoutSeconds * 1000;

        while (System.currentTimeMillis() < deadline) {
            File[] files = dir.listFiles(f -> f.isFile() && f.length() > 0 && !isPartialDownload(f));
            if (files != null && files.length > 0) {
                return files[0];
            }
            sleep();
        }
        throw new RuntimeException(
            "[DownloadManager] No file appeared in " + dir.getAbsolutePath() +
            " within " + timeoutSeconds + " seconds."
        );
    }

    /**
     * Deletes all files in the download directory.
     * Safe to call between tests to ensure a clean state.
     */
    public static void clearDownloads() {
        File dir = resolveDownloadDir();
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) f.delete();
        }
    }

    /**
     * Returns the configured download directory, creating it if it does not exist.
     */
    public static File resolveDownloadDir() {
        String configuredDir = SeleniumBootContext.getConfig().getBrowser().getDownloadDir();
        Path path = Paths.get(configuredDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        File dir = path.toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Partial downloads from Chrome (.crdownload) and Firefox (.part). */
    private static boolean isPartialDownload(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".crdownload") || name.endsWith(".part");
    }

    private static void sleep() {
        try { Thread.sleep(POLL_INTERVAL_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
