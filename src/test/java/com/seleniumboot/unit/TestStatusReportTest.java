package com.seleniumboot.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumboot.reporting.HtmlReportGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/** Retry/history status in the report: a section by the Flakiness Radar, newly failing first. */
public class TestStatusReportTest {

    private static String call(String method, String json, Object... extra) throws Exception {
        JsonNode tests = new ObjectMapper().readTree(json);
        Class<?>[] types = extra.length == 0
                ? new Class<?>[]{JsonNode.class}
                : new Class<?>[]{JsonNode.class, boolean.class};
        Method m = HtmlReportGenerator.class.getDeclaredMethod(method, types);
        m.setAccessible(true);
        Object[] args = extra.length == 0 ? new Object[]{tests} : new Object[]{tests, extra[0]};
        return (String) m.invoke(null, args);
    }

    private static String test(String id, String cls, String status, String testStatus) {
        return "{\"testId\":\"" + id + "\",\"testClassName\":\"" + cls + "\",\"status\":\"" + status + "\""
                + (testStatus == null ? "" : ",\"testStatus\":\"" + testStatus + "\"") + "}";
    }

    @Test
    public void section_listsFlaggedTestsWithNewlyFailingFirst() throws Exception {
        String html = call("buildTestStatusSection", "["
                + test("p.A.retry", "A", "PASSED", "PASSED_ON_RETRY") + ","
                + test("p.B.fresh", "B", "FAILED", "NEWLY_FAILING") + ","
                + test("p.C.plain", "C", "PASSED", null) + "]");
        Assert.assertTrue(html.contains("Newly failing") && html.contains("Passed on retry"), html);
        Assert.assertTrue(html.contains("fresh") && html.contains("retry"), html);
        Assert.assertFalse(html.contains("plain"), html);
        Assert.assertTrue(html.indexOf("Newly failing") < html.indexOf("Passed on retry"), html);
    }

    @Test
    public void section_isEmptyWhenNothingIsFlagged() throws Exception {
        Assert.assertEquals(call("buildTestStatusSection", "[" + test("p.A.a", "A", "PASSED", null) + "]"), "");
    }

    @Test
    public void failureList_putsNewlyFailingFirst() throws Exception {
        String html = call("buildFailureRows", "["
                + test("p.A.old", "ClassA", "FAILED", "CONSISTENTLY_FAILING") + ","
                + test("p.B.fresh", "ClassB", "FAILED", "NEWLY_FAILING") + ","
                + test("p.A.plain", "ClassA", "FAILED", null) + "]", false);
        int fresh = html.indexOf("ClassB");
        int a = html.indexOf("ClassA");
        Assert.assertTrue(fresh >= 0 && a >= 0 && fresh < a, "ClassB (newly failing) must come first: " + html);
    }

    @Test
    public void failureList_keepsOrderWhenNothingIsNewlyFailing() throws Exception {
        String html = call("buildFailureRows", "["
                + test("p.A.a", "ClassA", "FAILED", null) + ","
                + test("p.B.b", "ClassB", "FAILED", "CONSISTENTLY_FAILING") + "]", false);
        Assert.assertTrue(html.indexOf("ClassA") < html.indexOf("ClassB"), html);
    }
}
