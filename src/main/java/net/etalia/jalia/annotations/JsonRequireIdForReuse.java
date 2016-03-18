package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the presence of the "id" in the incoming JSON to reuse existing instance.
 * 
 * <p>
 * Normally, Jalia tries to reuse the existing instance instead of creating a new one, unless 
 * there is no existing instance or the "id" found in the JSON is different than the one in the 
 * database.
 * </p>
 * <p>
 * However, if there is no "id" in the JSON, by default Jalia reuses the existing instance.
 * </p>
 * <p>
 * This annotation change this last behavior, requiring the "id" to be in the JSON (and
 * to be the same found in the database) to reuse the instance, otherwise a new instance
 * will be created, and depending on the underlying data model the old instance could
 * be dropped.
 * </p>
 * 
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface JsonRequireIdForReuse {

}
