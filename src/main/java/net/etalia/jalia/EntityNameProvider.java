package net.etalia.jalia;

/**
 * An EntityNameProvider is responsible of converting between classes and JSON "@class" names.
 */
public interface EntityNameProvider {

	/**
	 * Get the JSON name of an entity of the given class.
	 * @param clazz The class to convert to JSON name.
	 * @return The JSON name to use in the "@class" attribute.
	 */
	String getEntityName(Class<?> clazz);

	/**
	 * Get the entity class corresponding to a JSON name.
	 * @param name The JSON name found in a "@class" attribute.
	 * @return The correct Entity class to use.
	 */
	Class<?> getEntityClass(String name);
		
}
