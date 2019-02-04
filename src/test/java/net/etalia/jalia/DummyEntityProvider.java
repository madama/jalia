package net.etalia.jalia;

import java.util.HashMap;
import java.util.Map;

import net.etalia.jalia.EntityFactory;
import net.etalia.jalia.EntityNameProvider;
import net.etalia.jalia.JsonClassData;
import net.etalia.jalia.DummyAddress.AddressType;

public class DummyEntityProvider implements EntityFactory, EntityNameProvider, JsonClassDataFactory {

	private Map<String, DummyEntity> db = new HashMap<>();
	
	private JsonClassDataFactoryImpl classFactory = new JsonClassDataFactoryImpl();
	
	public void addToDb(DummyEntity... entities) {
		for (DummyEntity de : entities) {
			if (de.getIdentifier() == null) throw new IllegalArgumentException("No id on " + de);
			db.put(de.getIdentifier(), de);
		}
	}
	
	@Override
	public JsonClassData getClassData(Class<?> clazz, JsonContext context) {
		JsonClassData jcd = classFactory.getClassData(clazz, context);
		if (!jcd.isNew()) return jcd;
		if (DummyEntity.class.isAssignableFrom(clazz)) {
			jcd.ignoreSetter("identifier");
			jcd.ignoreGetter("identifier");
		}
		jcd.unsetNew();
		return jcd;
	}
	
	@Override
	public String getEntityName(Class<?> clazz) {
		if (clazz == DummyPerson.class) return "Person";
		if (clazz == DummyAddress.class) return "Address";
		throw new IllegalArgumentException("Can't find a name for " + clazz);
	}

	@Override
	public Class<?> getEntityClass(String name) {
		if (name.equals("Person")) return DummyPerson.class;
		if (name.equals("Address")) return DummyAddress.class;
		throw new IllegalArgumentException("Can't find a class for " + name);
	}

	@Override
	public String getId(Object entity, JsonContext context) {
		if (!(entity instanceof DummyEntity)) throw new IllegalArgumentException("Cannot get id from " + entity);
		return ((DummyEntity)entity).getIdentifier();
	}

	@Override
	public Object buildEntity(Class<?> clazz, Object id, JsonContext context) {
		if (clazz == null) return null;
		DummyEntity ret = null;
		if (id != null) {
			ret = db.get(id);
			if (ret != null) return ret;
		}
		try {
			if (clazz == DummyAddress.class) {
				ret = new DummyAddress(id.toString(), null, null);
			} else if (clazz == DummyPerson.class) {
				ret = new DummyPerson();
			} else {
				ret = (DummyEntity)TypeUtil.get(clazz).newInstance();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (id != null) {
			ret.setIdentifier(id.toString());
		}
		return ret;
	}

	@Override
	public Object prepare(Object obj, boolean serializing, JsonContext context) {
		return obj;
	}

	@Override
	public Object finish(Object obj, boolean serializing, JsonContext context) {
		return obj;
	}
	
}
