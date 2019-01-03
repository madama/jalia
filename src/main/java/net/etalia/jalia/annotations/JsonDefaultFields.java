package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies which fields must be serialized by default when no fields are requested by the calling client.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface JsonDefaultFields {
	
	String UNASSIGNED = "[unassigned]";

	String value() default UNASSIGNED;

}
