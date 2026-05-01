package com.seleniumboot.cucumber;

import com.seleniumboot.steps.StepLogger;
import com.seleniumboot.steps.StepStatus;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;

/**
 * Cucumber plugin that pipes Gherkin step names into Selenium Boot's
 * {@link StepLogger}, making them visible in the HTML report step timeline.
 *
 * <p>Register in {@code @CucumberOptions}:
 * <pre>
 * plugin = {"pretty", "com.seleniumboot.cucumber.CucumberStepLogger"}
 * </pre>
 *
 * <p>Only Gherkin steps ({@link PickleStepTestStep}) are logged.
 * {@link HookTestStep} entries ({@code @Before}, {@code @After}, etc.) are skipped
 * to keep the timeline focused on scenario steps.
 *
 * <p>Implements {@link ConcurrentEventListener} (not {@link io.cucumber.plugin.EventListener})
 * for thread-safety during parallel scenario execution.
 */
public final class CucumberStepLogger implements ConcurrentEventListener {

    private static final ThreadLocal<String> CURRENT_STEP = new ThreadLocal<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
    }

    private void onStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof HookTestStep) {
            CURRENT_STEP.set(null);
            return;
        }
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            CURRENT_STEP.set(step.getStepText());
        }
    }

    private void onStepFinished(TestStepFinished event) {
        if (event.getTestStep() instanceof HookTestStep) return;

        String stepName = CURRENT_STEP.get();
        CURRENT_STEP.remove();
        if (stepName == null) return;

        Result result = event.getResult();
        StepStatus status = mapStatus(result.getStatus());

        // Take a screenshot on failure so it appears in the step timeline
        boolean screenshot = (status == StepStatus.FAIL);
        StepLogger.step(stepName, status, screenshot);
    }

    private static StepStatus mapStatus(Status cucumberStatus) {
        return switch (cucumberStatus) {
            case PASSED             -> StepStatus.PASS;
            case FAILED             -> StepStatus.FAIL;
            case SKIPPED, PENDING,
                 UNDEFINED, AMBIGUOUS -> StepStatus.WARN;
            default                 -> StepStatus.INFO;
        };
    }
}
