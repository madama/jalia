package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly defines a method as a getter to use for Json serialization (and also for deserialization of
 * collections and other entities, where the getter is called to fetch the existing value).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonGetter {
    /**
     * Defines name of the property, no value means that
     * name should be derived from the method.
     */
    String value() default "";
}
