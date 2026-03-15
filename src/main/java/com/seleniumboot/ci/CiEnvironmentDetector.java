package com.seleniumboot.ci;

import java.io.File;

/**
 * Detects whether the current process is running inside a CI environment or
 * a Docker/Kubernetes container. Used by the framework to auto-apply
 * CI-friendly defaults (headless, sandbox flags, thread tuning).
 */
public final class CiEnvironmentDetector {

    private CiEnvironmentDetector() {}

    // ==========================================================
    // CI Detection
    // ==========================================================

    /**
     * Returns true if any well-known CI environment variable is set.
     * Covers GitHub Actions, Jenkins, Travis CI, CircleCI, GitLab CI,
     * TeamCity, and Bitbucket Pipelines.
     */
    public static boolean isCI() {
        return isEnvSet("CI")
                || isEnvSet("GITHUB_ACTIONS")
                || isEnvSet("JENKINS_URL")
                || isEnvSet("TRAVIS")
                || isEnvSet("CIRCLECI")
                || isEnvSet("GITLAB_CI")
                || isEnvSet("TEAMCITY_VERSION")
                || isEnvSet("BITBUCKET_BUILD_NUMBER");
    }

    /**
     * Returns a human-readable name for the detected CI provider.
     */
    public static String ciName() {
        if (isEnvSet("GITHUB_ACTIONS"))        return "GitHub Actions";
        if (isEnvSet("JENKINS_URL"))           return "Jenkins";
        if (isEnvSet("TRAVIS"))                return "Travis CI";
        if (isEnvSet("CIRCLECI"))              return "CircleCI";
        if (isEnvSet("GITLAB_CI"))             return "GitLab CI";
        if (isEnvSet("TEAMCITY_VERSION"))      return "TeamCity";
        if (isEnvSet("BITBUCKET_BUILD_NUMBER")) return "Bitbucket Pipelines";
        if (isEnvSet("CI"))                    return "CI (generic)";
        return "local";
    }

    // ==========================================================
    // Container Detection
    // ==========================================================

    /**
     * Returns true if the process is running inside a Docker container or
     * a Kubernetes pod. Used to auto-apply Chrome sandbox / shared-memory flags.
     */
    public static boolean isContainer() {
        // Docker writes /.dockerenv on startup
        if (new File("/.dockerenv").exists()) {
            return true;
        }
        // Kubernetes injects KUBERNETES_SERVICE_HOST into every pod
        if (isEnvSet("KUBERNETES_SERVICE_HOST")) {
            return true;
        }
        // Fallback: inspect /proc/1/cgroup for "docker" or "kubepods" (Linux only)
        File cgroupFile = new File("/proc/1/cgroup");
        if (cgroupFile.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(cgroupFile.toPath()));
                return content.contains("docker") || content.contains("kubepods");
            } catch (Exception ignored) {
                // not readable — skip
            }
        }
        return false;
    }

    // ==========================================================
    // Thread Tuning
    // ==========================================================

    /**
     * Returns the recommended thread count for CI execution.
     * Uses available CPU cores, capped at {@code maxAllowed}.
     */
    public static int recommendedThreadCount(int maxAllowed) {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.min(cores, maxAllowed);
    }

    // ==========================================================
    // Internal
    // ==========================================================

    private static boolean isEnvSet(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}
