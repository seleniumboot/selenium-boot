package com.seleniumboot.unit;

import com.seleniumboot.quarantine.QuarantineLoader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link QuarantineLoader}.
 * Uses a temp YAML file and the system property override to avoid touching the real
 * working-directory file.
 */
public class QuarantineTest {

    private File tempFile;

    @BeforeMethod
    public void setup() throws Exception {
        QuarantineLoader.reload();
        tempFile = File.createTempFile("selenium-quarantine-", ".yml");
        tempFile.deleteOnExit();
    }

    @AfterMethod
    public void cleanup() {
        System.clearProperty("selenium.boot.quarantine");
        QuarantineLoader.reload();
        if (tempFile != null) tempFile.delete();
    }

    // ── plain string entries ───────────────────────────────────────────────

    @Test
    public void plainEntry_classMethod_isQuarantined() throws Exception {
        write("quarantine:\n  - com.example.LoginTest#loginTest\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
    }

    @Test
    public void plainEntry_classOnly_matchesAllMethods() throws Exception {
        write("quarantine:\n  - com.example.PaymentTest\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.PaymentTest#checkout"));
        assertTrue(QuarantineLoader.isQuarantined("com.example.PaymentTest#refund"));
    }

    @Test
    public void plainEntry_classOnly_doesNotMatchOtherClass() throws Exception {
        write("quarantine:\n  - com.example.PaymentTest\n");
        assertFalse(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
    }

    @Test
    public void notInFile_returnsNotQuarantined() throws Exception {
        write("quarantine:\n  - com.example.LoginTest#loginTest\n");
        assertFalse(QuarantineLoader.isQuarantined("com.example.OtherTest#otherMethod"));
    }

    // ── structured entries ─────────────────────────────────────────────────

    @Test
    public void structuredEntry_isQuarantined() throws Exception {
        write("quarantine:\n  - test: com.example.SearchTest#searchSpecial\n    reason: \"JIRA-42\"\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.SearchTest#searchSpecial"));
    }

    @Test
    public void structuredEntry_reasonIsReturned() throws Exception {
        write("quarantine:\n  - test: com.example.SearchTest#searchSpecial\n    reason: \"JIRA-42 unicode bug\"\n");
        assertEquals(QuarantineLoader.getReason("com.example.SearchTest#searchSpecial"), "JIRA-42 unicode bug");
    }

    @Test
    public void structuredEntry_noReason_returnsDefaultMessage() throws Exception {
        write("quarantine:\n  - test: com.example.SearchTest#searchSpecial\n");
        String reason = QuarantineLoader.getReason("com.example.SearchTest#searchSpecial");
        assertNotNull(reason);
        assertFalse(reason.isEmpty());
        assertTrue(reason.contains("selenium-quarantine.yml"));
    }

    // ── class-level reason lookup ──────────────────────────────────────────

    @Test
    public void classOnlyEntry_reasonAppliedToMethodLookup() throws Exception {
        write("quarantine:\n  - test: com.example.PaymentTest\n    reason: \"Payment gateway unstable\"\n");
        assertEquals(
            QuarantineLoader.getReason("com.example.PaymentTest#checkout"),
            "Payment gateway unstable"
        );
    }

    // ── mixed format ───────────────────────────────────────────────────────

    @Test
    public void mixedFormat_bothFormatsWorkTogether() throws Exception {
        write("quarantine:\n" +
              "  - com.example.LoginTest#loginTest\n" +
              "  - test: com.example.SearchTest#search\n" +
              "    reason: \"Flaky\"\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
        assertTrue(QuarantineLoader.isQuarantined("com.example.SearchTest#search"));
        assertEquals(QuarantineLoader.getReason("com.example.SearchTest#search"), "Flaky");
    }

    // ── empty / missing file ───────────────────────────────────────────────

    @Test
    public void emptyFile_nothingIsQuarantined() throws Exception {
        write("");
        assertFalse(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
        assertEquals(QuarantineLoader.size(), 0);
    }

    @Test
    public void emptyList_nothingIsQuarantined() throws Exception {
        write("quarantine: []\n");
        assertFalse(QuarantineLoader.isQuarantined("anything#method"));
        assertEquals(QuarantineLoader.size(), 0);
    }

    @Test
    public void missingFile_nothingIsQuarantined() {
        // No file set — system property is cleared, no working-dir file, no classpath file
        // (classpath has selenium-quarantine-test.yml but NOT selenium-quarantine.yml)
        assertFalse(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
        assertEquals(QuarantineLoader.size(), 0);
    }

    // ── caching ────────────────────────────────────────────────────────────

    @Test
    public void caching_fileReadOnlyOnce() throws Exception {
        write("quarantine:\n  - com.example.LoginTest#loginTest\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
        // Second call uses cache — delete file to prove it's not re-read
        tempFile.delete();
        assertTrue(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
    }

    @Test
    public void reload_clearsCache() throws Exception {
        write("quarantine:\n  - com.example.LoginTest#loginTest\n");
        assertTrue(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));

        QuarantineLoader.reload();
        // After reload with no file set, should return false
        System.clearProperty("selenium.boot.quarantine");
        assertFalse(QuarantineLoader.isQuarantined("com.example.LoginTest#loginTest"));
    }

    // ── Cucumber: tag matching ────────────────────────────────────────────

    @Test
    public void cucumber_tagEntry_matchesScenarioWithThatTag() throws Exception {
        write("quarantine:\n  - \"@smoke\"\n");
        List<String> tags = Arrays.asList("@smoke", "@login");
        assertTrue(QuarantineLoader.isQuarantinedScenario(tags, "classpath:login.feature", "Login"));
    }

    @Test
    public void cucumber_tagEntry_caseInsensitive() throws Exception {
        write("quarantine:\n  - \"@Smoke\"\n");
        List<String> tags = Collections.singletonList("@smoke");
        assertTrue(QuarantineLoader.isQuarantinedScenario(tags, "classpath:login.feature", "Login"));
    }

    @Test
    public void cucumber_tagEntry_noAtPrefixInYaml_stillMatches() throws Exception {
        // Entry stored without @ (user might omit it) — handled by startsWith("@") check
        // If the entry is "smoke" (no @), it won't be treated as a tag entry
        // but "@smoke" will match scenarios with tag "@smoke" or "smoke"
        write("quarantine:\n  - \"@smoke\"\n");
        // scenario tag without @ prefix
        List<String> tags = Collections.singletonList("smoke");
        assertTrue(QuarantineLoader.isQuarantinedScenario(tags, "classpath:login.feature", "Login"));
    }

    @Test
    public void cucumber_tagEntry_doesNotMatchDifferentTag() throws Exception {
        write("quarantine:\n  - \"@smoke\"\n");
        List<String> tags = Collections.singletonList("@regression");
        assertFalse(QuarantineLoader.isQuarantinedScenario(tags, "classpath:login.feature", "Login"));
    }

    @Test
    public void cucumber_tagEntry_withReason() throws Exception {
        write("quarantine:\n  - test: \"@regression\"\n    reason: \"Regression suite broken\"\n");
        List<String> tags = Collections.singletonList("@regression");
        assertTrue(QuarantineLoader.isQuarantinedScenario(tags, "classpath:payment.feature", "Pay"));
        assertEquals(QuarantineLoader.getScenarioReason(tags, "classpath:payment.feature", "Pay"),
                "Regression suite broken");
    }

    // ── Cucumber: feature file matching ──────────────────────────────────

    @Test
    public void cucumber_featureFile_matchesAllScenariosInFile() throws Exception {
        write("quarantine:\n  - login.feature\n");
        List<String> noTags = Collections.emptyList();
        assertTrue(QuarantineLoader.isQuarantinedScenario(noTags, "classpath:features/login.feature", "Any scenario"));
        assertTrue(QuarantineLoader.isQuarantinedScenario(noTags, "classpath:features/login.feature", "Another scenario"));
    }

    @Test
    public void cucumber_featureFile_doesNotMatchDifferentFile() throws Exception {
        write("quarantine:\n  - login.feature\n");
        List<String> noTags = Collections.emptyList();
        assertFalse(QuarantineLoader.isQuarantinedScenario(noTags, "classpath:features/payment.feature", "Pay"));
    }

    @Test
    public void cucumber_featureFile_withSubPath_matchesUri() throws Exception {
        write("quarantine:\n  - features/payment.feature\n");
        List<String> noTags = Collections.emptyList();
        assertTrue(QuarantineLoader.isQuarantinedScenario(noTags, "classpath:features/payment.feature", "Pay"));
    }

    @Test
    public void cucumber_featureFile_withReason() throws Exception {
        write("quarantine:\n  - test: payment.feature\n    reason: \"Gateway down\"\n");
        List<String> noTags = Collections.emptyList();
        assertTrue(QuarantineLoader.isQuarantinedScenario(noTags, "classpath:features/payment.feature", "Pay"));
        assertEquals(QuarantineLoader.getScenarioReason(noTags, "classpath:features/payment.feature", "Pay"),
                "Gateway down");
    }

    // ── Cucumber: feature#scenario-name matching ──────────────────────────

    @Test
    public void cucumber_featureHashName_matchesSpecificScenario() throws Exception {
        write("quarantine:\n  - \"checkout.feature#Checkout with 3D Secure\"\n");
        List<String> noTags = Collections.emptyList();
        assertTrue(QuarantineLoader.isQuarantinedScenario(
                noTags, "classpath:features/checkout.feature", "Checkout with 3D Secure"));
    }

    @Test
    public void cucumber_featureHashName_doesNotMatchDifferentScenario() throws Exception {
        write("quarantine:\n  - \"checkout.feature#Checkout with 3D Secure\"\n");
        List<String> noTags = Collections.emptyList();
        assertFalse(QuarantineLoader.isQuarantinedScenario(
                noTags, "classpath:features/checkout.feature", "Checkout as guest"));
    }

    @Test
    public void cucumber_featureHashName_scenarioNameCaseInsensitive() throws Exception {
        write("quarantine:\n  - \"login.feature#Login with expired session\"\n");
        List<String> noTags = Collections.emptyList();
        assertTrue(QuarantineLoader.isQuarantinedScenario(
                noTags, "classpath:login.feature", "LOGIN WITH EXPIRED SESSION"));
    }

    // ── matchesScenario static helper ─────────────────────────────────────

    @Test
    public void matchesScenario_javaEntry_returnsFalse() {
        // Java class entries must never match Cucumber scenarios
        List<String> tags = Collections.singletonList("@smoke");
        assertFalse(QuarantineLoader.matchesScenario(
                "com.example.LoginTest#loginTest", tags, "classpath:login.feature", "Login"));
        assertFalse(QuarantineLoader.matchesScenario(
                "com.example.LoginTest", tags, "classpath:login.feature", "Login"));
    }

    @Test
    public void featureUriMatches_variousUriFormats() {
        assertTrue(QuarantineLoader.featureUriMatches("login.feature",
                "classpath:features/login.feature"));
        assertTrue(QuarantineLoader.featureUriMatches("features/login.feature",
                "classpath:features/login.feature"));
        assertTrue(QuarantineLoader.featureUriMatches("login.feature",
                "file:///home/ci/project/src/test/resources/features/login.feature"));
        assertFalse(QuarantineLoader.featureUriMatches("login.feature",
                "classpath:features/payment.feature"));
    }

    // ── size ───────────────────────────────────────────────────────────────

    @Test
    public void size_reflectsNumberOfEntries() throws Exception {
        write("quarantine:\n  - com.example.A#a\n  - com.example.B\n  - test: com.example.C#c\n    reason: \"x\"\n");
        assertEquals(QuarantineLoader.size(), 3);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void write(String content) throws Exception {
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(content);
        }
        System.setProperty("selenium.boot.quarantine", tempFile.getAbsolutePath());
    }
}
