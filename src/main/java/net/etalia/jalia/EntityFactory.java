package net.etalia.jalia;

public interface EntityFactory {

	public String getId(Object entity, JsonContext context);
	
	public Object buildEntity(Class<?> clazz, String id, JsonContext context);
	
	public Object prepare(Object obj, boolean serializing, JsonContext context);
	
	public Object finish(Object obj, boolean serializing, JsonContext context);
	
	
}
