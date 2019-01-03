package net.etalia.jalia;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link JsonClassDataFactory}.
 *
 * This implementation will cache based on {@link Class} and create vanilla {@link JsonClassData} using
 * {@link JsonClassData#JsonClassData(Class)} constructor.
 */
public class JsonClassDataFactoryImpl implements JsonClassDataFactory {

	private ConcurrentHashMap<Class<?>, JsonClassData> cache = new ConcurrentHashMap<Class<?>, JsonClassData>();
	
	@Override
	public JsonClassData getClassData(Class<?> clazz, JsonContext context) {
		JsonClassData ret = cache.get(clazz);
		if (ret != null) return ret;
		ret = new JsonClassData(clazz);
		cache.put(clazz, ret);
		return ret;
	}
	
	public void cache(Class<?> clazz, JsonClassData cd) {
		cache.put(clazz, cd);
	}

}
