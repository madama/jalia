package net.etalia.jalia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.etalia.jalia.ObjectMapper;

/**
 * Annotation used to indicate when value of the annotated property (when
 * used for a field, method or constructor parameter), or all 
 * properties of the annotated class, is to be serialized.
 * Without annotation property values are included depending on
 * {@link ObjectMapper#setSendEmpty(boolean)} and {@link ObjectMapper#setSendNulls(boolean)}.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD,
    ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonInclude {

	public Include value() default Include.ALWAYS;
    
    /**
     * Enumeration used with {@link JsonInclude}
     */
    public enum Include {
        /**
         * The property is to be always included, even if null or empty
         */
        ALWAYS,

        /**
         * The property has to be included if not null, even if empty
         */
        NOT_NULL,

        /**
         * The property has to be included only if is not empty, will be omitted if null or empty.
         */
        NOT_EMPTY
    }

}