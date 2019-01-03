package net.etalia.jalia;

import java.io.IOException;

/**
 * A JsonDeSer is responsible for serializing and deserializing a value to JSON.
 */
public interface JsonDeSer {

	/**
	 * Key used by all serializers to check for circular serialization.
	 */
	String CTX_ALL_SERIALIZESTACK = "All_SerializeStack";

	/**
	 * Check whether this instance can serialize a class in the current serialization context.
	 * @param context The current serialization context.
	 * @param clazz The clazz that needs to be serialized.
	 * @return a score, where any number below zero means not to use this serializer at all, any number equal or above
	 * 10 means to use this serializer without searching any further, while any other intermediate value makes this
	 * instance score better or worse than others.
	 */
	int handlesSerialization(JsonContext context, Class<?> clazz);

	/**
	 * Check whether this instance can deserialize a type in the current deserialization context.
	 * @param context The current deserialization context.
	 * @param hint A hint on the type that needs to be deserialized.
	 * @return a score, where any number below zero means not to use this deserializer at all, any number equal or above
	 * 10 means to use this deserializer without searching any further, while any other intermediate value makes this
	 * instance score better or worse than others.
	 */
	int handlesDeserialization(JsonContext context, TypeUtil hint);

	/**
	 * Serialize an object in the current serialization context.
	 * @param obj The object to serialize.
	 * @param context The current serialization context.
	 * @throws IOException if an error occurs on the underlying stream.
	 */
	void serialize(Object obj, JsonContext context) throws IOException;

	/**
	 * Deserialize from the current deserialization context into an object.
	 * @param context The current deserialization context.
	 * @param pre The existing value to modify if any, null otherwise.
	 * @param hint A hint ont he expected return type.
	 * @return The deserialized value.
	 * @throws IOException if an error occurs on the underlying stream.
	 */
	Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException;
	
}
