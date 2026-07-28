package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.execution.ExecutionValidator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ExecutionValidator}.
 */
public class ExecutionValidatorTest {

    private SeleniumBootConfig.Execution execution(String parallel, int threadCount) {
        SeleniumBootConfig.Execution execution = new SeleniumBootConfig.Execution();
        execution.setParallel(parallel);
        execution.setThreadCount(threadCount);
        return execution;
    }

    private String messageOf(SeleniumBootConfig.Execution execution) {
        try {
            ExecutionValidator.validate(execution);
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
        fail("expected validate() to throw");
        return null;
    }

    // ----------------------------------------------------------
    // parallel — accepted values
    // ----------------------------------------------------------

    @DataProvider(name = "validParallelModes")
    public Object[][] validParallelModes() {
        return new Object[][]{{"none"}, {"methods"}, {"classes"}, {"tests"}, {"instances"}};
    }

    /**
     * Every mode TestNG's own XmlSuite.ParallelMode accepts is accepted here — the value
     * is handed straight to XmlSuite.setParallel() downstream. "tests" and "instances"
     * were rejected before the fix for issue #35.
     */
    @Test(dataProvider = "validParallelModes")
    public void validate_acceptsEveryTestNGParallelMode(String mode) {
        ExecutionValidator.validate(execution(mode, 1));
    }

    @Test
    public void validate_parallelIsCaseInsensitive() {
        ExecutionValidator.validate(execution("METHODS", 1));
        ExecutionValidator.validate(execution("Tests", 1));
    }

    /**
     * The validator must accept exactly what SuiteExecutionListener can later parse —
     * both uppercase via Locale.ROOT — or the two drift and a "valid" value blows up at
     * suite start instead of at validation.
     */
    @Test
    public void validate_untrimmedParallel_isRejectedLikeDownstream() {
        String message = messageOf(execution("  classes  ", 1));
        assertTrue(message.contains("classes"), "was: " + message);
    }

    // ----------------------------------------------------------
    // parallel — rejected values
    // ----------------------------------------------------------

    /**
     * Regression test for issue #35, bug 2: an unsupported-but-present value used to
     * report "Parallel execution configuration missing", sending the user hunting for
     * config that was not absent. The message must name the offending value.
     */
    @Test
    public void validate_unknownParallel_messageNamesRejectedValue() {
        String message = messageOf(execution("threads", 1));
        assertTrue(message.contains("threads"),
                "message should name the rejected value, was: " + message);
        assertFalse(message.contains("missing"),
                "a present-but-invalid value is not a missing one, was: " + message);
    }

    @Test
    public void validate_unknownParallel_messageListsValidValues() {
        String message = messageOf(execution("threads", 1));
        for (String valid : new String[]{"none", "methods", "classes", "tests", "instances"}) {
            assertTrue(message.contains(valid),
                    "message should list '" + valid + "', was: " + message);
        }
    }

    @Test
    public void validate_nullParallel_reportsMissing() {
        assertEquals(messageOf(execution(null, 1)), "Parallel execution configuration missing");
    }

    @Test
    public void validate_blankParallel_reportsMissing() {
        assertEquals(messageOf(execution("   ", 1)), "Parallel execution configuration missing");
    }

    // ----------------------------------------------------------
    // execution block / threadCount
    // ----------------------------------------------------------

    @Test
    public void validate_nullExecution_reportsMissing() {
        assertEquals(messageOf(null), "Execution configuration missing");
    }

    @Test
    public void validate_threadCountBelowOne_isRejected() {
        assertEquals(messageOf(execution("methods", 0)), "Thread count must be >= 1");
    }

    @Test
    public void validate_threadCountAboveSafeLimit_isRejected() {
        int overLimit = Runtime.getRuntime().availableProcessors() * 2 + 1;
        String message = messageOf(execution("methods", overLimit));
        assertTrue(message.contains("exceeds safe limit"), "was: " + message);
        assertTrue(message.contains(String.valueOf(overLimit)), "was: " + message);
    }
}
