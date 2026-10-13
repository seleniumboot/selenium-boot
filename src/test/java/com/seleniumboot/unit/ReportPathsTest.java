package com.seleniumboot.unit;

import com.seleniumboot.reporting.ReportPaths;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ReportPaths} — the report output location resolver that
 * lets multiple test engines in one build avoid overwriting each other's report.
 */
public class ReportPathsTest {

    @AfterMethod
    public void clearOverride() {
        System.clearProperty("seleniumboot.reports.dir");
        System.clearProperty("seleniumboot.history.dir");
    }

    @Test
    public void historyDir_systemPropertyMovesHistoryOutOfBaseDir() {
        System.setProperty("seleniumboot.reports.dir", "target/junit5");
        System.setProperty("seleniumboot.history.dir", "/ci/cache/history");
        assertEquals(ReportPaths.metricsHistoryDir().getPath(), new File("/ci/cache/history").getPath());
    }

    @Test
    public void historyDir_configSettingIsUsedWhenNoSystemProperty() {
        com.seleniumboot.config.SeleniumBootConfig cfg = new com.seleniumboot.config.SeleniumBootConfig();
        com.seleniumboot.config.SeleniumBootConfig.Flakiness flakiness =
                new com.seleniumboot.config.SeleniumBootConfig.Flakiness();
        flakiness.setHistoryDir("  .sb-history ");
        cfg.setFlakiness(flakiness);
        try (org.mockito.MockedStatic<com.seleniumboot.internal.SeleniumBootContext> ctx =
                     org.mockito.Mockito.mockStatic(com.seleniumboot.internal.SeleniumBootContext.class)) {
            ctx.when(com.seleniumboot.internal.SeleniumBootContext::getConfig).thenReturn(cfg);
            assertEquals(ReportPaths.metricsHistoryDir().getPath(), ".sb-history");

            System.setProperty("seleniumboot.history.dir", "/from/property");
            assertEquals(ReportPaths.metricsHistoryDir().getPath(), new File("/from/property").getPath(),
                    "the system property wins over the config");
        }
    }

    @Test
    public void historyDir_blankConfigKeepsTheDefaultLocation() {
        System.clearProperty("seleniumboot.reports.dir");
        com.seleniumboot.config.SeleniumBootConfig cfg = new com.seleniumboot.config.SeleniumBootConfig();
        cfg.setFlakiness(new com.seleniumboot.config.SeleniumBootConfig.Flakiness());
        try (org.mockito.MockedStatic<com.seleniumboot.internal.SeleniumBootContext> ctx =
                     org.mockito.Mockito.mockStatic(com.seleniumboot.internal.SeleniumBootContext.class)) {
            ctx.when(com.seleniumboot.internal.SeleniumBootContext::getConfig).thenReturn(cfg);
            assertEquals(ReportPaths.metricsHistoryDir().getPath(),
                    "target/metrics-history".replace('/', File.separatorChar));
        }
    }

    @Test
    public void baseDir_defaultsToTarget_whenNoOverride() {
        System.clearProperty("seleniumboot.reports.dir");
        assertEquals(ReportPaths.baseDir(), "target");
    }

    @Test
    public void baseDir_honorsOverride() {
        System.setProperty("seleniumboot.reports.dir", "target/junit5");
        assertEquals(ReportPaths.baseDir(), "target/junit5");
    }

    @Test
    public void baseDir_trimsOverride() {
        System.setProperty("seleniumboot.reports.dir", "  target/junit5  ");
        assertEquals(ReportPaths.baseDir(), "target/junit5");
    }

    @Test
    public void baseDir_ignoresBlankOverride() {
        System.setProperty("seleniumboot.reports.dir", "   ");
        assertEquals(ReportPaths.baseDir(), "target");
    }

    @Test
    public void metricsJson_isUnderBaseDir() {
        System.setProperty("seleniumboot.reports.dir", "target/junit5");
        assertEquals(ReportPaths.metricsJson().getPath(),
                "target/junit5/selenium-boot-metrics.json".replace('/', java.io.File.separatorChar));
    }

    @Test
    public void htmlReport_isUnderBaseDir() {
        System.setProperty("seleniumboot.reports.dir", "target/junit5");
        assertEquals(ReportPaths.htmlReport().getPath(),
                "target/junit5/selenium-boot-report.html".replace('/', java.io.File.separatorChar));
    }

    @Test
    public void metricsHistory_isUnderBaseDir() {
        System.setProperty("seleniumboot.reports.dir", "target/junit5");
        assertEquals(ReportPaths.metricsHistoryDir().getPath(),
                "target/junit5/metrics-history".replace('/', java.io.File.separatorChar));
    }

    @Test
    public void defaults_matchHistoricalTargetPaths() {
        System.clearProperty("seleniumboot.reports.dir");
        assertEquals(ReportPaths.metricsJson().getPath(),
                "target/selenium-boot-metrics.json".replace('/', java.io.File.separatorChar));
        assertEquals(ReportPaths.htmlReport().getPath(),
                "target/selenium-boot-report.html".replace('/', java.io.File.separatorChar));
    }

    // --- build-tool detection (#81): must not depend on whether target/ already exists ---

    private static File project(String... files) throws Exception {
        File dir = Files.createTempDirectory("sb-layout").toFile();
        for (String f : files) {
            File file = new File(dir, f);
            if (f.endsWith("/")) file.mkdirs(); else file.createNewFile();
        }
        return dir;
    }

    @Test
    public void gradleProject_isGradleLayout_evenWhenTargetExists() throws Exception {
        // the #81 case: a component created target/ before the path was resolved
        assertTrue(ReportPaths.usesGradleLayout(project("build.gradle", "build/", "target/")));
    }

    @Test
    public void gradleKotlinProject_isGradleLayout() throws Exception {
        assertTrue(ReportPaths.usesGradleLayout(project("build.gradle.kts")));
    }

    @Test
    public void mavenProject_isNotGradleLayout() throws Exception {
        assertFalse(ReportPaths.usesGradleLayout(project("pom.xml", "target/")));
    }

    @Test
    public void bothBuildFiles_mavenWins() throws Exception {
        assertFalse(ReportPaths.usesGradleLayout(project("pom.xml", "build.gradle")));
    }

    @Test
    public void noBuildFile_fallsBackToBuildDirHeuristic() throws Exception {
        assertTrue(ReportPaths.usesGradleLayout(project("build/")));
        assertFalse(ReportPaths.usesGradleLayout(project("build/", "target/")));
        assertFalse(ReportPaths.usesGradleLayout(project()));
    }

    @Test
    public void resolve_isUnderBaseDir() {
        System.setProperty("seleniumboot.reports.dir", "out");
        assertEquals(ReportPaths.resolve("recordings").getPath(), "out" + File.separator + "recordings");
    }
}
