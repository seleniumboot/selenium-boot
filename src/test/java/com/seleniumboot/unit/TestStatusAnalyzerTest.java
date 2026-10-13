package com.seleniumboot.unit;

import com.seleniumboot.flakiness.TestStatus;
import com.seleniumboot.flakiness.TestStatusAnalyzer;
import com.seleniumboot.flakiness.TestStatusAnalyzer.Outcome;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Retry- and history-aware status: a pure function, then the history-file wiring around it. */
public class TestStatusAnalyzerTest {

    private static final Outcome FAIL = new Outcome("FAILED", 0);
    private static final Outcome PASS = new Outcome("PASSED", 0);

    private static TestStatus classify(Outcome current, String... previous) {
        return TestStatusAnalyzer.classify(current, List.of(previous));
    }

    // ------------------------------------------------------------------ pure function

    @Test
    public void passedOnRetry_isFlagged_evenWithNoHistory() {
        Assert.assertEquals(classify(new Outcome("PASSED", 2)), TestStatus.PASSED_ON_RETRY);
    }

    @Test
    public void passedFirstTime_getsNoStatus_whateverTheHistory() {
        Assert.assertNull(classify(PASS));
        Assert.assertNull(classify(PASS, "FAILED", "PASSED", "FAILED"));
    }

    @Test
    public void failedAfterEveryEarlierRunPassed_isNewlyFailing() {
        Assert.assertEquals(classify(FAIL, "PASSED", "PASSED", "PASSED"), TestStatus.NEWLY_FAILING);
    }

    @Test
    public void failedAfterASinglePassedRun_isNewlyFailing() {
        Assert.assertEquals(classify(FAIL, "PASSED"), TestStatus.NEWLY_FAILING);
    }

    @Test
    public void failedInEveryEarlierRun_isConsistentlyFailing() {
        Assert.assertEquals(classify(FAIL, "FAILED", "FAILED"), TestStatus.CONSISTENTLY_FAILING);
    }

    @Test
    public void failedTwiceInARow_isNotYetConsistent() {
        Assert.assertNull(classify(FAIL, "FAILED"));
    }

    @Test
    public void failedWithMixedHistory_isIntermittent() {
        Assert.assertEquals(classify(FAIL, "PASSED", "FAILED", "PASSED"), TestStatus.INTERMITTENT);
    }

    @Test
    public void failedWithNoHistory_hasNoStatus() {
        Assert.assertNull(classify(FAIL));
        Assert.assertNull(TestStatusAnalyzer.classify(FAIL, null));
    }

    @Test
    public void failedAfterRetries_isStillJudgedByHistory() {
        Assert.assertEquals(classify(new Outcome("FAILED", 2), "PASSED", "PASSED"), TestStatus.NEWLY_FAILING);
    }

    @Test
    public void skippedRunsInHistory_areNotEvidence() {
        Assert.assertEquals(classify(FAIL, "SKIPPED", "PASSED", "SKIPPED"), TestStatus.NEWLY_FAILING);
        Assert.assertNull(classify(FAIL, "SKIPPED", "SKIPPED"));
    }

    @Test
    public void skippedOrMissingCurrent_hasNoStatus() {
        Assert.assertNull(classify(new Outcome("SKIPPED", 0), "PASSED"));
        Assert.assertNull(TestStatusAnalyzer.classify(null, List.of("PASSED")));
    }

    // ------------------------------------------------------------------ history files

    private Path dir;
    private String previousProperty;

    @BeforeMethod
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("status-history");
        previousProperty = System.getProperty("seleniumboot.reports.dir");
        System.setProperty("seleniumboot.reports.dir", dir.toString());
        Files.createDirectories(dir.resolve("metrics-history"));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (previousProperty == null) System.clearProperty("seleniumboot.reports.dir");
        else System.setProperty("seleniumboot.reports.dir", previousProperty);
        File[] files = dir.resolve("metrics-history").toFile().listFiles();
        if (files != null) for (File f : files) f.delete();
        Files.deleteIfExists(dir.resolve("metrics-history"));
        Files.deleteIfExists(dir);
    }

    private void run(String name, long modified, String... idStatus) throws Exception {
        StringBuilder sb = new StringBuilder("{\"tests\":[");
        for (int i = 0; i < idStatus.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"testId\":\"").append(idStatus[i]).append("\",\"status\":\"")
              .append(idStatus[i + 1]).append("\"}");
        }
        Path f = dir.resolve("metrics-history").resolve(name);
        Files.writeString(f, sb.append("]}").toString());
        Assert.assertTrue(f.toFile().setLastModified(modified));
    }

    @Test
    public void emptyHistoryDirectory_onlyRetriesAreFlagged() {
        Map<String, TestStatus> r = TestStatusAnalyzer.forRun(
                Map.of("A.a", FAIL, "B.b", new Outcome("PASSED", 1)), Set.of());
        Assert.assertEquals(r, Map.of("B.b", TestStatus.PASSED_ON_RETRY));
    }

    @Test
    public void historyIsReadPerTest() throws Exception {
        run("r1.json", 1000, "A.a", "PASSED", "B.b", "FAILED", "C.c", "PASSED");
        run("r2.json", 2000, "A.a", "PASSED", "B.b", "FAILED", "C.c", "FAILED");
        run("r3.json", 3000, "A.a", "PASSED", "B.b", "FAILED", "C.c", "PASSED");

        Map<String, TestStatus> r = TestStatusAnalyzer.forRun(
                Map.of("A.a", FAIL, "B.b", FAIL, "C.c", FAIL, "D.d", FAIL), Set.of());

        Assert.assertEquals(r.get("A.a"), TestStatus.NEWLY_FAILING);
        Assert.assertEquals(r.get("B.b"), TestStatus.CONSISTENTLY_FAILING);
        Assert.assertEquals(r.get("C.c"), TestStatus.INTERMITTENT);
        Assert.assertNull(r.get("D.d"), "a test with no history gets no status");
    }

    @Test
    public void historyWrittenByThisRun_isNotComparedWithItself() throws Exception {
        run("earlier.json", 1000, "A.a", "PASSED");
        run("this-run.json", 2000, "A.a", "FAILED");

        Assert.assertEquals(TestStatusAnalyzer.forRun(Map.of("A.a", FAIL), Set.of()).get("A.a"),
                TestStatus.INTERMITTENT, "sanity: without the exclusion this run is counted");
        Assert.assertEquals(TestStatusAnalyzer.forRun(Map.of("A.a", FAIL), Set.of("this-run.json")).get("A.a"),
                TestStatus.NEWLY_FAILING);
    }

    @Test
    public void unreadableHistoryFile_isSkipped() throws Exception {
        Files.writeString(dir.resolve("metrics-history").resolve("broken.json"), "{ not json");
        run("ok.json", 5000, "A.a", "PASSED");
        Assert.assertEquals(TestStatusAnalyzer.forRun(Map.of("A.a", FAIL), Set.of()).get("A.a"),
                TestStatus.NEWLY_FAILING);
    }
}
