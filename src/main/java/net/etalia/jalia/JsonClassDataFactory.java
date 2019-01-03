package net.etalia.jalia;

/**
 * A JSON Class Data Factory produces JsonClassData for a given Class.
 *
 * See {@link JsonClassDataFactoryImpl} for the default implementation, suitable for most cases.
 */
public interface JsonClassDataFactory {

	/**
	 * Create or retrieve a JsonClassData object for the given class and json context.
	 * @param clazz The class of the entity.
	 * @param context The current JSON de-serialization context.
	 * @return the JsonClassData object.
	 */
	JsonClassData getClassData(Class<?> clazz, JsonContext context);
}
