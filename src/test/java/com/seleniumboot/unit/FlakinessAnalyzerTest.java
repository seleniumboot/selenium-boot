package com.seleniumboot.unit;

import com.seleniumboot.flakiness.FlakinessScore;
import com.seleniumboot.flakiness.FlakinessScore.Risk;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link FlakinessScore} classification logic.
 * Pure math — no I/O or browser required.
 */
public class FlakinessAnalyzerTest {

    // ------------------------------------------------------------------
    // Risk classification thresholds
    // ------------------------------------------------------------------

    @Test
    public void classify_zeroPercent_isStable() {
        Assert.assertEquals(FlakinessScore.classify(0.0, 33.0), Risk.STABLE);
    }

    @Test
    public void classify_belowTen_isStable() {
        Assert.assertEquals(FlakinessScore.classify(9.9, 33.0), Risk.STABLE);
    }

    @Test
    public void classify_exactlyTen_isWatch() {
        Assert.assertEquals(FlakinessScore.classify(10.0, 33.0), Risk.WATCH);
    }

    @Test
    public void classify_midRange_isWatch() {
        Assert.assertEquals(FlakinessScore.classify(25.0, 33.0), Risk.WATCH);
    }

    @Test
    public void classify_atThreshold_isHigh() {
        Assert.assertEquals(FlakinessScore.classify(33.0, 33.0), Risk.HIGH);
    }

    @Test
    public void classify_above100_isHigh() {
        Assert.assertEquals(FlakinessScore.classify(100.0, 33.0), Risk.HIGH);
    }

    @Test
    public void classify_customThreshold50_watch() {
        Assert.assertEquals(FlakinessScore.classify(40.0, 50.0), Risk.WATCH);
    }

    @Test
    public void classify_customThreshold50_high() {
        Assert.assertEquals(FlakinessScore.classify(50.0, 50.0), Risk.HIGH);
    }

    // ------------------------------------------------------------------
    // FlakinessScore value object
    // ------------------------------------------------------------------

    @Test
    public void flakinessScore_storesAllFields() {
        FlakinessScore s = new FlakinessScore("com.example.LoginTest.login", 20, 5, 25.0, Risk.WATCH);
        Assert.assertEquals(s.getTestId(),      "com.example.LoginTest.login");
        Assert.assertEquals(s.getRunsAnalysed(), 20);
        Assert.assertEquals(s.getFailCount(),    5);
        Assert.assertEquals(s.getFailureRate(),  25.0, 0.001);
        Assert.assertEquals(s.getRisk(),         Risk.WATCH);
    }

    @Test
    public void flakinessScore_100PercentFail_isHigh() {
        FlakinessScore s = new FlakinessScore("t", 10, 10, 100.0, Risk.HIGH);
        Assert.assertEquals(s.getRisk(), Risk.HIGH);
    }

    @Test
    public void flakinessScore_zeroFails_isStable() {
        FlakinessScore s = new FlakinessScore("t", 10, 0, 0.0, Risk.STABLE);
        Assert.assertEquals(s.getRisk(), Risk.STABLE);
    }

    // ------------------------------------------------------------------
    // AiFailureAnalyzer — extractContent
    // ------------------------------------------------------------------

    @Test
    public void extractContent_parsesTextFromClaudeResponse() {
        String json = "{"
            + "\"id\":\"msg_01\","
            + "\"type\":\"message\","
            + "\"content\":[{\"type\":\"text\",\"text\":\"Root cause: element not found.\"}]"
            + "}";
        String result = com.seleniumboot.ai.AiFailureAnalyzer.extractContent(json);
        Assert.assertEquals(result, "Root cause: element not found.");
    }

    @Test
    public void extractContent_handlesEscapedNewlines() {
        String json = "{\"content\":[{\"type\":\"text\",\"text\":\"Line1\\nLine2\"}]}";
        String result = com.seleniumboot.ai.AiFailureAnalyzer.extractContent(json);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("Line1") && result.contains("Line2"));
    }

    @Test
    public void extractContent_missingTextField_returnsNull() {
        String json = "{\"content\":[{\"type\":\"image\"}]}";
        Assert.assertNull(com.seleniumboot.ai.AiFailureAnalyzer.extractContent(json));
    }

    @Test
    public void extractContent_emptyJson_returnsNull() {
        Assert.assertNull(com.seleniumboot.ai.AiFailureAnalyzer.extractContent("{}"));
    }
}
