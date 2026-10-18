package com.seleniumboot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.metrics.TestTiming;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class AiRunSummarizerTest {

    private static TestTiming timing(String id, String status, String error) {
        TestTiming t = new TestTiming(id, "main");
        t.setStatus(status);
        t.setErrorMessage(error);
        t.setStackTrace(error == null ? null
                : "java.lang.AssertionError: " + error + "\n\tat example.Page.check(Page.java:10)\n");
        return t;
    }

    @Test
    public void noFailures_noPrompt() {
        assertNull(AiRunSummarizer.buildPrompt(List.of(timing("a.One.run", "PASSED", null))));
    }

    @Test
    public void sameCauseFailures_areGrouped_largestFirst() {
        List<TestTiming> all = new ArrayList<>();
        all.add(timing("a.One.run", "FAILED", "login API returned 500"));
        all.add(timing("a.Two.run", "FAILED", "login API returned 503"));
        all.add(timing("a.Three.run", "FAILED", "login API returned 500"));
        all.add(timing("a.Other.run", "FAILED", "cart badge missing"));
        all.add(timing("a.Fine.run", "PASSED", null));

        String prompt = AiRunSummarizer.buildPrompt(all);

        assertTrue(prompt.contains("- Tests: 5"), prompt);
        assertTrue(prompt.contains("- Failed: 4"), prompt);
        assertTrue(prompt.contains("Distinct failure causes: 2"), prompt);
        assertTrue(prompt.indexOf("Cause 1 (3 tests)") >= 0, "numbers normalise, so 500/503 group together: " + prompt);
        assertTrue(prompt.indexOf("Cause 1 (3 tests)") < prompt.indexOf("Cause 2 (1 test)"));
        assertFalse(prompt.contains("a.Fine.run"), "passing tests are not sent");
    }

    @Test
    public void groupsAndTestsAreCapped() {
        List<TestTiming> all = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String msg = "distinct cause number " + "x".repeat(i + 1) + " failed";
            all.add(timing("a.T" + i + ".run", "FAILED", msg));
        }
        String prompt = AiRunSummarizer.buildPrompt(all);
        assertTrue(prompt.contains("(4 smaller causes omitted)"), prompt);

        List<TestTiming> same = new ArrayList<>();
        for (int i = 0; i < 9; i++) same.add(timing("a.S" + i + ".run", "FAILED", "same boom"));
        assertTrue(AiRunSummarizer.buildPrompt(same).contains("(+4 more)"));
    }

    @Test
    public void disabledByDefault_returnsNull() {
        assertNull(AiRunSummarizer.summarize(List.of(timing("a.One.run", "FAILED", "boom"))));
    }
}
