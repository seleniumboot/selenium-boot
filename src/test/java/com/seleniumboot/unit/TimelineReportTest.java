package com.seleniumboot.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class TimelineReportTest {

    private static String timeline(String json) throws Exception {
        JsonNode tests = new ObjectMapper().readTree(json);
        return timelineNode(tests);
    }

    private static String timelineNode(JsonNode tests) throws Exception {
        Method m = com.seleniumboot.reporting.HtmlReportGenerator.class
                .getDeclaredMethod("buildTimelineSection", JsonNode.class);
        m.setAccessible(true);
        return (String) m.invoke(null, tests);
    }

    private static int count(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    @Test
    public void rendersOneLanePerThreadUsingRealTimestamps() throws Exception {
        String html = timeline("""
                [
                  {"testId":"com.x.A.fast","thread":"pool-1-thread-1","status":"PASSED",
                   "totalMs":100,"startTimeMs":1000,"endTimeMs":1100},
                  {"testId":"com.x.A.slow","thread":"pool-1-thread-1","status":"FAILED",
                   "totalMs":400,"startTimeMs":1100,"endTimeMs":1500},
                  {"testId":"com.x.B.other","thread":"pool-1-thread-2","status":"PASSED",
                   "totalMs":250,"startTimeMs":1050,"endTimeMs":1300}
                ]
                """);

        Assert.assertTrue(html.contains("timeline-card"), html);
        Assert.assertTrue(html.contains("pool-1-thread-1"), html);
        Assert.assertTrue(html.contains("pool-1-thread-2"), html);
        Assert.assertEquals(count(html, "gantt-row gantt-lane\""), 2, html);
        Assert.assertTrue(html.contains("gantt-bar-passed"), html);
        Assert.assertTrue(html.contains("gantt-bar-failed"), html);
        Assert.assertTrue(html.contains("left:"), html);
        Assert.assertTrue(html.contains("width:"), html);
        Assert.assertFalse(html.contains("timeline-note"),
                "real timestamps must not trigger the estimate note: " + html);
    }

    @Test
    public void overlappingThreadsArePositionedOnTheSameAxis() throws Exception {
        String html = timeline("""
                [
                  {"testId":"a","thread":"t1","status":"PASSED","totalMs":100,"startTimeMs":1000,"endTimeMs":1100},
                  {"testId":"b","thread":"t2","status":"PASSED","totalMs":100,"startTimeMs":1000,"endTimeMs":1100}
                ]
                """);
        // Both start at the same instant, so both bars sit at the left edge.
        Assert.assertEquals(count(html, "gantt-bar-passed\" style=\"left:0.000%"), 2, html);
    }

    @Test
    public void fallsBackToSequentialEstimateWithoutStartTimes() throws Exception {
        String html = timeline("""
                [
                  {"testId":"a","thread":"main","status":"PASSED","totalMs":100},
                  {"testId":"b","thread":"main","status":"PASSED","totalMs":300}
                ]
                """);

        Assert.assertTrue(html.contains("timeline-note"), html);
        Assert.assertTrue(html.contains("gantt-bar-passed"), html);
        // a: 0-100 of a 400ms lane -> second bar starts at 25%
        Assert.assertTrue(html.contains("left:25.000%"), html);
        Assert.assertTrue(html.contains("width:75.000%"), html);
    }

    @Test
    public void handlesSingleThreadRun() throws Exception {
        String html = timeline("""
                [
                  {"testId":"a","thread":"main","status":"PASSED","totalMs":100,"startTimeMs":1000,"endTimeMs":1100},
                  {"testId":"b","thread":"main","status":"PASSED","totalMs":200,"startTimeMs":1100,"endTimeMs":1300}
                ]
                """);

        Assert.assertEquals(count(html, "gantt-row gantt-lane\""), 1, html);
        Assert.assertEquals(count(html, "gantt-bar "), 2, html);
        Assert.assertFalse(html.contains("timeline-note"), html);
    }

    @Test
    public void emptyStateWhenNoTimingData() throws Exception {
        String html = timeline("""
                [
                  {"testId":"a","status":"PASSED","totalMs":0},
                  {"testId":"b","thread":"main","status":"SKIPPED"}
                ]
                """);

        Assert.assertTrue(html.contains("timeline-empty"), html);
        Assert.assertTrue(html.contains("No timing data available"), html);
        Assert.assertFalse(html.contains("gantt-bar "), html);
    }

    @Test
    public void missingThreadUsesUnknownLane() throws Exception {
        String html = timeline("""
                [{"testId":"a","status":"PASSED","totalMs":100,"startTimeMs":1000,"endTimeMs":1100}]
                """);

        Assert.assertTrue(html.contains(">unknown<"), html);
    }

    @Test
    public void nullOrEmptyTestsRenderNothing() throws Exception {
        Assert.assertEquals(timelineNode(null), "");
        Assert.assertEquals(timeline("[]"), "");
    }
}
