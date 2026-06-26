package com.seleniumboot.unit;

import com.seleniumboot.reporting.ReportPaths;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ReportPaths} — the report output location resolver that
 * lets multiple test engines in one build avoid overwriting each other's report.
 */
public class ReportPathsTest {

    @AfterMethod
    public void clearOverride() {
        System.clearProperty("seleniumboot.reports.dir");
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
}
