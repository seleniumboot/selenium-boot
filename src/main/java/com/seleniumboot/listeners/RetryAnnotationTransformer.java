package com.seleniumboot.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Injects {@link RetryListener} on every test method.
 *
 * <p>Injecting on all methods is intentional: {@link RetryListener#retry} enforces
 * the master kill switch and max-attempts limit at runtime, so this transformer
 * does not need to know which methods should actually retry. Selective retry
 * behaviour is controlled entirely through configuration and the {@link Retryable}
 * annotation — see {@link RetryListener} for the decision logic.
 */
public final class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {

        if (testMethod != null) {
            annotation.setRetryAnalyzer(RetryListener.class);
        }
    }
}