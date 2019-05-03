package net.etalia.jalia;

/**
 * Default options supported by {@link ObjectMapper}.
 */
public enum DefaultOptions implements Option<Boolean> {

	/**
	 * If set to true, null values will be serialized, otherwise by default null values will not be sent.
	 *
	 * @see net.etalia.jalia.annotations.JsonInclude
	 */
	INCLUDE_NULLS,

	/**
	 * If set to true, empty lists and empty maps will be serialized, otherwise by default they will not.
	 *
	 * @see net.etalia.jalia.annotations.JsonInclude
	 */
	INCLUDE_EMPTY,

	/**
	 * If set to true, output will be pretty printed.
	 */
	PRETTY_PRINT,

	/**
	 * If set to true, readers will be lenient and accept partially malformed JSONs. Note that jsons consisting of only
	 * a native value (like, a json consisting of only <pre>true</pre> or <pre>1</pre>) are automatically detected and
	 * parsed accordingly.
	 */
	LENIENT_READER,

	/**
	 * Always "unroll" (that is, serialize completely) linked entities, instead of only sending their ids.
	 */
	UNROLL_OBJECTS,

	/**
	 * Record object changes while deserializing, see {@link ChangeRecorder}.
	 */
	RECORD_CHANGES,

	/**
	 * Always serializes properties annotated with @{@link net.etalia.jalia.annotations.JsonOnDemandOnly}.
	 */
	ALWAYS_SERIALIZE_ON_DEMAND_ONLY,

	/**
	 * Always allows creation of new instances, as if all methods were annotated with {@link
	 * net.etalia.jalia.annotations.JsonAllowNewInstances}
	 */
	ALWAYS_ALLOW_NEW_INSTANCES,

	/**
	 * Always allows entity property changes, as if all methods were annotated with {@link
	 * net.etalia.jalia.annotations.JsonAllowEntityPropertyChanges}
	 */
	ALWAYS_ALLOW_ENTITY_PROPERTY_CHANGES,

	/**
	 * Override getters and setters annotated with {@link net.etalia.jalia.annotations.JsonIgnore}, and always serialize
	 * and deserialize them.
	 */
	OVERRIDE_IGNORES
}
