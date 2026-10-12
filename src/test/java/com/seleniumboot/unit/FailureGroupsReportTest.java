package com.seleniumboot.unit;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/** The failures tab lists shared causes above the flat list. */
public class FailureGroupsReportTest {

    private static String groups(String json) throws Exception {
        com.fasterxml.jackson.databind.JsonNode tests = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        Method m = com.seleniumboot.reporting.HtmlReportGenerator.class
                .getDeclaredMethod("buildFailureGroups", com.fasterxml.jackson.databind.JsonNode.class);
        m.setAccessible(true);
        return (String) m.invoke(null, tests);
    }

    private static String failed(String id, String signature) {
        return "{\"testId\":\"" + id + "\",\"status\":\"FAILED\",\"errorMessage\":\"gone\","
                + "\"failureCategory\":\"LOCATOR_NOT_FOUND\",\"failureEvidence\":\"x\","
                + "\"failureSignature\":\"" + signature + "\"}";
    }

    @Test
    public void sharedCause_isListedWithCount() throws Exception {
        String html = groups("[" + failed("A.a", "s1") + "," + failed("B.b", "s1") + "," + failed("C.c", "s2") + "]");
        Assert.assertTrue(html.contains("2 tests, same cause"), html);
        Assert.assertTrue(html.contains("A.a") && html.contains("B.b"), html);
        Assert.assertFalse(html.contains("C.c"), html);
    }

    @Test
    public void noSharedCause_rendersNothing() throws Exception {
        Assert.assertEquals(groups("[" + failed("A.a", "s1") + "," + failed("C.c", "s2") + "]"), "");
    }

    @Test
    public void passedTestsAreIgnored() throws Exception {
        Assert.assertEquals(groups("[{\"testId\":\"A.a\",\"status\":\"PASSED\",\"failureSignature\":\"s1\"},"
                + "{\"testId\":\"B.b\",\"status\":\"PASSED\",\"failureSignature\":\"s1\"}]"), "");
    }
}
