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
	 * Always "unroll" (that is, serialize completely) linked entities, instead of only sending their ids.
	 */
	UNROLL_OBJECTS,

	/**
	 * Record object changes while deserializing, see {@link ChangeRecorder}.
	 */
	RECORD_CHANGES
	
}
