package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that a given getter or setter should be ignored.
 * 
 * If placed on a setter, also the getter is ignore, and the opposite is also true.
 * 
 * If placed on a setter (or getter), and don't want the other one to be ignored, use
 * on the ohter one JsonIgnore(false) or an explicit JsonGetter or JsonSetter annotation.
 * 
 * @author Simone Gianni <simoneg@apache.org>
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore {
    /**
     * Optional argument for use with "annotation overrides".
     */
    boolean value() default true;
}
