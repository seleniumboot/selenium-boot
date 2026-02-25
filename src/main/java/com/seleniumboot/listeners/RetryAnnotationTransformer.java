package com.seleniumboot.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Injects {@link RetryListener} on any test method annotated with {@link Retryable}.
 *
 * This transformer is the only wiring between the {@code @Retryable} annotation
 * and TestNG's retry mechanism. It does not read config — retry count and the
 * master kill switch are enforced inside {@link RetryListener} at runtime.
 */
public final class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {

        if (testMethod != null && testMethod.isAnnotationPresent(Retryable.class)) {
            annotation.setRetryAnalyzer(RetryListener.class);
        }
    }
}