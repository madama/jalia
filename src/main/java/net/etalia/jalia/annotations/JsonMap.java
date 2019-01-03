package net.etalia.jalia.annotations;

import java.lang.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instructs Jalia to drop an existing map, or to clear it (calling {@link Map#clear()} before
 * deserializing it, or to retain objects found in the collection but not on the json.
 * <p>
 * Normally, for consistency with the existing model (like preserving existing Hibernate
 * PersistentCollections), the existing map, if any, is preserved during deserialization and
 * elements are added or removed inside the map.
 * </p>
 * <p>
 * However there are situations where this is not possible or can lead to errors :
 * <ul>
 *   <li>When a map is not modifiable, because the getter wraps the collection with {@link Collections#unmodifiableMap(Map)}</li>
 *   <li>Because of custom implementations of {@link Map} that can't be updated correctly.</li>
 *   <li>Because the map is acting as a metadata structure, and existing elements should not be removed.</li>
 * </ul>
 * </p>
 * @author Simone Gianni <simoneg@apache.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface JsonMap {

    /**
     * If set to true, elements found on the existing map but not found in the json will be retained and not removed.
     * @return
     */
    boolean retain() default false;

    /**
     * If set to true, the existing found collection will not be reused.
     */
    boolean drop() default false;

    /**
     * If set to true, the existing found collection will be cleared, so objects matching (by index) will not be reused.
     * @return
     */
    boolean clear() default false;
}
