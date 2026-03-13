package com.seleniumboot.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation that activates {@link SeleniumBootExtension} on a test class.
 *
 * <p>Equivalent to {@code @ExtendWith(SeleniumBootExtension.class)} but more expressive:
 * <pre>{@code
 * @EnableSeleniumBoot
 * class LoginTest extends BaseJUnit5Test {
 *     @Test
 *     void loginSucceeds() {
 *         open();
 *         // ...
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SeleniumBootExtension.class)
public @interface EnableSeleniumBoot {
}
