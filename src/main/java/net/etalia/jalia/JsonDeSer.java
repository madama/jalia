package net.etalia.jalia;

import java.io.IOException;

public interface JsonDeSer {

	public int handlesSerialization(JsonContext context, Class<?> clazz);
	
	public int handlesDeserialization(JsonContext context, TypeUtil hint);
	
	public void serialize(Object obj, JsonContext context) throws IOException;
	
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException;
	
}
