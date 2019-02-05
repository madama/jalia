package net.etalia.jalia.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that for this property or collection, it is allowed to modify values of the referenced entities, otherwise
 * default behavior is not to allow modifications and silently ignore the case (cause the client could be simply using
 * stale data). Note that this is applied only to entities (that is, when {@link net.etalia.jalia.EntityFactory} is
 * used and is returning an entity. This annotation cascades via collections, but does not cascade via chains of
 * getters/setters.
 *
 * @author Simone Gianni <simoneg@apache.org>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAllowEntityPropertyChanges {

}