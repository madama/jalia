package net.etalia.jalia.annotations;


import net.etalia.jalia.OutField;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that for this property or collection, it is allowed to create a new
 * entity, otherwise default behavior is to allow existing entities only. This annotation cascades
 * thru collections, but does not cascade thru chains of getters/setters.
 *
 * @author Simone Gianni <simoneg@apache.org>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAllowNewEntities {

}