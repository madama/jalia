package net.etalia.jalia;

/**
 * An EntityFactory is responsible with dealing with entities in the context of a json de-serialization.
 *
 * EntityFactories will convert to and from "id"s and real entities, for example loading entities from underlying
 * DB or finding them in any other way suitable to the application model.
 *
 *
 */
public interface EntityFactory {

	/**
	 * Converts the given entity to an "id" to be used when serializing the entity to Json.
	 * @param entity The entity to be serialized.
	 * @param context The current JsonContext.
	 * @return a string representation of the "id" of this entity.
	 */
	String getId(Object entity, JsonContext context);

	/**
	 * Builds an entity of the given class, for the given "id", during deserialization. The returned entity will then
	 * be eventually modified based on the JSON content.
	 * @param clazz The class of the entity needed.
	 * @param id The id of the entity, if any is provided.
	 * @param context The current JsonContext.
	 * @return A live entity that will eventually then be manipulated (by calling setters and getters) by the
	 * deserializer.
	 */
	Object buildEntity(Class<?> clazz, String id, JsonContext context);

	/**
	 * Generic hook called by {@link BeanJsonDeSer} before starting de-serialization of an entity.
	 * @param obj The entity being serialized or deserialized.
	 * @param serializing true if serializing, false if deserializing.
	 * @param context The current JsonContext.
	 * @return The same entity or possibily also a different one if needed.
	 */
	Object prepare(Object obj, boolean serializing, JsonContext context);

	/**
	 * Generic hook called by {@link BeanJsonDeSer} after de-serialization of an entity is finished.
	 * @param obj The entity that was serialized or deserialized.
	 * @param serializing true if serializing, false if deserializing.
	 * @param context The current JsonContext.
	 * @return The same entity or possibily also a different one if needed.
	 */
	Object finish(Object obj, boolean serializing, JsonContext context);
}
