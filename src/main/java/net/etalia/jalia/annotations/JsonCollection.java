package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Instructs Jalia to drop an existing collection or to clear it (calling {@link List#clear()} before
 * deserializing it. 
 * <p>
 * Normally, for consistency with the existing model (like preserving existing Hibernate 
 * PersistentCollections), the existing collection, if any, is preserved during deserialization and 
 * elements are added, removed or reordered inside the collection.
 * </p>
 * <p>
 * However there are situations where this is not possible or can lead to errors :
 * <ul>
 *   <li>When a collection is not modifiable, because the getter wraps the collection with {@link Collections#unmodifiableCollection(java.util.Collection)}</li>
 *   <li>Because of custom implementations of {@link List} or {@link Set} that can't be updated correctly.</li>
 *   <li>Because of Hibernate bugs in reordering a collection with OrderColumn</li>
 * </ul>
 * </p>
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface JsonCollection {

	public boolean drop() default false;
	public boolean clear() default false;
	
}
