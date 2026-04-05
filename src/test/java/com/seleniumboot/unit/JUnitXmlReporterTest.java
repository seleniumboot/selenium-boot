package com.seleniumboot.unit;

import com.seleniumboot.metrics.TestTiming;
import com.seleniumboot.reporting.JUnitXmlReporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link JUnitXmlReporter}.
 * Verifies that the generated XML file exists and contains well-formed content.
 */
public class JUnitXmlReporterTest {

    private static final File XML_FILE =
            new File("target/surefire-reports/TEST-SeleniumBoot.xml");

    @AfterMethod
    public void cleanup() {
        if (XML_FILE.exists()) {
            XML_FILE.delete();
        }
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private TestTiming timing(String id, String status) {
        TestTiming t = new TestTiming(id, "main");
        t.setStatus(status);
        t.setTotalTime(250L);
        return t;
    }

    private String readXml() throws IOException {
        return Files.readString(XML_FILE.toPath());
    }

    // ----------------------------------------------------------
    // File creation
    // ----------------------------------------------------------

    @Test
    public void export_createsXmlFile() {
        JUnitXmlReporter.export(List.of(timing("t1", "PASSED")), 250L);
        assertTrue(XML_FILE.exists(), "XML report file should be created");
    }

    @Test
    public void export_emptyTimings_createsFile() {
        JUnitXmlReporter.export(List.of(), 0L);
        assertTrue(XML_FILE.exists());
    }

    // ----------------------------------------------------------
    // XML structure
    // ----------------------------------------------------------

    @Test
    public void export_containsXmlDeclaration() throws IOException {
        JUnitXmlReporter.export(List.of(timing("t1", "PASSED")), 500L);
        assertTrue(readXml().startsWith("<?xml"), "File must start with XML declaration");
    }

    @Test
    public void export_containsTestsuiteElement() throws IOException {
        JUnitXmlReporter.export(List.of(timing("t1", "PASSED")), 500L);
        assertTrue(readXml().contains("<testsuite"), "Must contain <testsuite> element");
    }

    @Test
    public void export_testCounts_matchTimings() throws IOException {
        List<TestTiming> timings = List.of(
                timing("t1", "PASSED"),
                timing("t2", "FAILED"),
                timing("t3", "SKIPPED")
        );
        JUnitXmlReporter.export(timings, 1000L);
        String xml = readXml();
        assertTrue(xml.contains("tests=\"3\""), "tests attribute should be 3");
        assertTrue(xml.contains("failures=\"1\""), "failures attribute should be 1");
        assertTrue(xml.contains("skipped=\"1\""), "skipped attribute should be 1");
    }

    @Test
    public void export_passedTest_isSelfClosingTestcase() throws IOException {
        JUnitXmlReporter.export(List.of(timing("myPassedTest", "PASSED")), 100L);
        String xml = readXml();
        assertTrue(xml.contains("name=\"myPassedTest\""));
        assertTrue(xml.contains("/>"), "Passed tests should be self-closing <testcase/>");
    }

    @Test
    public void export_failedTest_containsFailureElement() throws IOException {
        JUnitXmlReporter.export(List.of(timing("myFailedTest", "FAILED")), 100L);
        String xml = readXml();
        assertTrue(xml.contains("name=\"myFailedTest\""));
        assertTrue(xml.contains("<failure"), "Failed tests must contain <failure> element");
    }

    @Test
    public void export_skippedTest_containsSkippedElement() throws IOException {
        JUnitXmlReporter.export(List.of(timing("mySkippedTest", "SKIPPED")), 100L);
        String xml = readXml();
        assertTrue(xml.contains("name=\"mySkippedTest\""));
        assertTrue(xml.contains("<skipped/>"), "Skipped tests must contain <skipped/>");
    }

    // ----------------------------------------------------------
    // XML escaping
    // ----------------------------------------------------------

    @Test
    public void export_testIdWithSpecialChars_escapedProperly() throws IOException {
        JUnitXmlReporter.export(
                List.of(timing("test<with>&special\"chars", "PASSED")), 100L);
        String xml = readXml();
        assertFalse(xml.contains("<with>"),
                "Unescaped < > must not appear in attribute values");
        assertTrue(xml.contains("&lt;") || xml.contains("&amp;"),
                "Special chars must be XML-escaped");
    }

    // ----------------------------------------------------------
    // Failure message and stack trace content
    // ----------------------------------------------------------

    @Test
    public void failureElement_containsActualErrorMessage() throws IOException {
        TestTiming t = timing("failTest", "FAILED");
        t.setErrorMessage("Expected [Login] but found [Error 404]");
        JUnitXmlReporter.export(List.of(t), 100L);

        String xml = readXml();
        assertTrue(xml.contains("Expected [Login] but found [Error 404]"),
                "failure message attribute must contain actual error text");
    }

    @Test
    public void failureElement_containsStackTrace() throws IOException {
        TestTiming t = timing("failTest2", "FAILED");
        t.setErrorMessage("assertion failed");
        t.setStackTrace("java.lang.AssertionError: assertion failed\n\tat com.example.MyTest.myTest(MyTest.java:42)");
        JUnitXmlReporter.export(List.of(t), 100L);

        String xml = readXml();
        assertTrue(xml.contains("MyTest.java:42"),
                "failure element body must contain the stack trace");
    }

    // ----------------------------------------------------------
    // Idempotency — second call overwrites first
    // ----------------------------------------------------------

    @Test
    public void export_calledTwice_fileOverwritten() throws IOException {
        JUnitXmlReporter.export(List.of(timing("t1", "PASSED")), 100L);
        JUnitXmlReporter.export(List.of(timing("t2", "FAILED"), timing("t3", "FAILED")), 200L);
        String xml = readXml();
        assertTrue(xml.contains("tests=\"2\""),
                "Second export should overwrite with new content");
    }
}
