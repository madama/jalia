package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonGetter {
    /**
     * Defines name of the property, no value means that
     * name should be derived from the method.
     */
    String value() default "";
}
