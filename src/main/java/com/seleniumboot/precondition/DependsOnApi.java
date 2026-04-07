package com.seleniumboot.precondition;

import com.seleniumboot.api.SeleniumBootApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a test or class depends on an external API being reachable.
 *
 * <p>Before the test runs, Selenium Boot performs an HTTP GET to the given URL.
 * A 2xx response means the API is up. Any other response or a connection error
 * causes the test to be <strong>skipped</strong> rather than failed — keeping
 * the suite results clean when infrastructure is the problem.
 *
 * <p>Results are cached per URL for the entire suite run, so the same endpoint
 * is probed at most once regardless of how many tests depend on it.
 *
 * <p>Usage — on a class (all tests in the class share the guard):
 * <pre>
 * {@literal @}DependsOnApi("https://api.example.com/health")
 * public class PaymentTests extends BaseTest { ... }
 * </pre>
 *
 * <p>Usage — on a method (overrides any class-level annotation):
 * <pre>
 * {@literal @}DependsOnApi("https://auth.example.com/health")
 * {@literal @}Test
 * public void loginTest() { ... }
 * </pre>
 *
 * <p>Multiple dependencies (repeatable):
 * <pre>
 * {@literal @}DependsOnApi("https://api.example.com/health")
 * {@literal @}DependsOnApi("https://auth.example.com/health")
 * public class CheckoutTests extends BaseTest { ... }
 * </pre>
 */
@SeleniumBootApi(since = "1.3.0")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DependsOnApi.List.class)
public @interface DependsOnApi {

    /** URL to health-check. A 2xx response means the API is up. */
    String value();

    /** Connection and read timeout in seconds. Default: 5. */
    int timeoutSeconds() default 5;

    /** Container annotation enabling {@code @Repeatable} support. */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        DependsOnApi[] value();
    }
}
