package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify which properties to ignore during JSON serialization and deserialization.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnoreProperties
{
    /**
     * Names of properties to ignore.
     */
    String[] value() default { };

    /**
     * Unused. For compile compatibility only.
     */
    boolean ignoreUnknown() default false;
}
