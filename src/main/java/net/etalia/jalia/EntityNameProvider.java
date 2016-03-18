package net.etalia.jalia;

public interface EntityNameProvider {

	public String getEntityName(Class<?> clazz);
	
	public Class<?> getEntityClass(String name);
		
}
