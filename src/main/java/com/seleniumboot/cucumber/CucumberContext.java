package com.seleniumboot.cucumber;

import io.cucumber.java.Scenario;

/**
 * ThreadLocal holder for the current Cucumber {@link Scenario}.
 *
 * <p>Set by {@link CucumberHooks} at scenario start and cleared after scenario end.
 * Thread-safe for parallel scenario execution — each scenario thread holds its own instance.
 *
 * <p>Access from step definitions via {@link BaseCucumberSteps#getScenario()}.
 */
public final class CucumberContext {

    private static final ThreadLocal<Scenario> SCENARIO = new ThreadLocal<>();

    private CucumberContext() {}

    public static void setScenario(Scenario scenario) {
        SCENARIO.set(scenario);
    }

    public static Scenario getScenario() {
        return SCENARIO.get();
    }

    public static void clear() {
        SCENARIO.remove();
    }
}
