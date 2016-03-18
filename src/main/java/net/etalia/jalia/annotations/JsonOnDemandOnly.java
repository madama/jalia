package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.etalia.jalia.OutField;

/**
 * Denotes that a given getter must be called only if
 * explicitly required using {@link OutField}, and not
 * when serializing all fields.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonOnDemandOnly {

}
