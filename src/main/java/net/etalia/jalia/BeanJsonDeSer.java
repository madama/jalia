package net.etalia.jalia;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

public class BeanJsonDeSer implements JsonDeSer {

	public static final String REUSE_WITHOUT_ID = "BEAN_JSON_DESER_REUSEWITHOUTID";
	public static final String ALLOW_NEW = "BEAN_JSON_DESER_ALLOWNEW";
    public static final String ALLOW_CHANGES = "BEAN_JSON_DESER_ALLOWCHANGES";
	private static final String CTX_BEAN_JSON_DE_SER_DONES = "BeanJsonDeSer_Dones";
	private static final String CTX_BEAN_JSON_DE_SER_SENTS = "BeanJsonDeSer_Sents";

	private JsonDeSer idDeser = new NativeJsonDeSer();

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray()) return 0;
		return 5;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		JsonReader la = context.getInput().lookAhead();
		try {
			if (la.peek() != JsonToken.STRING && la.peek() != JsonToken.NULL) { 
				if (la.peek() != JsonToken.BEGIN_OBJECT) return -1;
				la.beginObject();
				while (la.hasNext()) {
					// TODO check is the @entity can be identified by the factory or not, before saying we can deserialize it!
					if (la.nextName().equals("@entity")) return 10;
					la.skipValue();
				}
			}
		} catch (IOException e) {
			// TODO what to do?
			e.printStackTrace();
		} finally {
			try {
				la.close();
			} catch (IOException e) {
				throw new IllegalStateException("Cannot close lookAhead json reader", e);
			}
		}
		
		// Map deserialize return 10 if hint say it's a map, and 5 if it's a json object, we get priority on concrete classes
		if (hint.isInstantiatable()) return 7;
		
		return -1;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		
		JsonWriter output = context.getOutput();
		
		ObjectMapper mapper = context.getMapper();
		EntityFactory factory = context.getMapper().getEntityFactory();
		if (factory != null) {
			obj = factory.prepare(obj, true, context);
			if (obj == null) {
				output.nullValue();
				return;
			}			
			
			Object id = factory.getId(obj, context);
			if (id != null) {
				// Prevent loops in serialization
				if (context.hasInLocalStack(CTX_ALL_SERIALIZESTACK, obj)) {
					if (!context.getFromStackBoolean(DefaultOptions.UNROLL_OBJECTS.toString()) || context.isSerializingAll()) {
						idDeser.serialize(id, context);
						return;
					}
				}
				
				// Prevent sending and object twice, send only the id, unless DefaultOptions.UNROLL_OBJECT
				Map<String,Object> sents = (Map<String, Object>) context.get(CTX_BEAN_JSON_DE_SER_SENTS);
				if (sents == null) {
					sents = new HashMap<>();
					context.put(CTX_BEAN_JSON_DE_SER_SENTS, sents);
				}
				if (sents.containsKey(id.toString()) && !context.getFromStackBoolean(DefaultOptions.UNROLL_OBJECTS.toString())) {
					idDeser.serialize(id, context);
					return;
				}
				sents.put(id.toString(), obj);
			}
		}

		if (context.hasInLocalStack(CTX_ALL_SERIALIZESTACK, obj)) {
			if (!context.getFromStackBoolean(DefaultOptions.UNROLL_OBJECTS.toString()) || context.isSerializingAll()) {
				// TODO this avoid loops, but also break serialization, cause there is no id to send
				output.clearName();
				return;
			}
		}
		
		context.putLocalStack(CTX_ALL_SERIALIZESTACK, obj);
		
		output.beginObject();
		boolean sentEntity = false;
		if (mapper.getEntityNameProvider() != null) {
			String entityName = mapper.getEntityNameProvider().getEntityName(obj.getClass());
			if (entityName != null) {
				output.name("@entity");
				output.value(entityName);
				sentEntity = true;
			}
		}
		if (!sentEntity) {
			output.name("@entity");
			output.value(obj.getClass().getSimpleName());
		}
		boolean idSent = false;
		if (factory != null) {
			Object id = factory.getId(obj, context);
			if (id != null) {
				output.name("id");
				idDeser.serialize(id, context);
				idSent = true;
			}
		}
		JsonClassData cd = context.getMapper().getClassDataFactory().getClassData(obj.getClass(), context);
		Set<String> sents = new HashSet<>();
		for (String name : cd.getSortedGettables()) {
			if (context.entering(name, cd.getDefaults())) {
				sents.add(name);
				output.name(name);
				context.putLocalStack(cd.getOptions(name));
				output.setSerializeNulls(context.getFromStackBoolean(DefaultOptions.INCLUDE_NULLS.toString()));
				try {
					context.getMapper().writeValue(cd.getValue(name, obj), context);
				} finally {
					context.exited();
				}
			}
		}
		for (String name : context.getCurrentSubs()) {
			if (sents.contains(name)) continue;
			if (idSent && name.equals("id")) continue;
			if (context.entering(name, cd.getDefaults())) {
				Object val;
				val = cd.getValue(name, obj);
				if (val == null && !context.getFromStackBoolean(DefaultOptions.INCLUDE_NULLS.toString())) {
					context.exited();
					continue;
				}
				context.putLocalStack(cd.getOptions(name));
				output.setSerializeNulls(context.getFromStackBoolean(DefaultOptions.INCLUDE_NULLS.toString()));
				output.name(name);
				try {
					context.getMapper().writeValue(val, context);
				} finally {
					context.exited();
				}
			}
		}
		output.endObject();
		
		if (factory != null) {
			factory.finish(obj, true, context);
		}
	}

	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		// Search for @entity and id
		Object id = null;
		String entity = null;
		boolean embedded = false;
		
		JsonReader input = context.getInput();
		EntityFactory factory = context.getMapper().getEntityFactory();				
		if (input.peek() == JsonToken.STRING) {
			// Embedded object
			id = input.nextString();
			embedded = true;
			// Search in already deserialized ones
			Map<String,Object> dones = (Map<String, Object>) context.get(CTX_BEAN_JSON_DE_SER_DONES);
			if (dones != null) {
				Object done = dones.get(id);
				if (done != null) return done;
			}
		} else if (input.peek() == JsonToken.NULL) {
			// null object, should not happen because native deser should take precedence
			input.nextNull();
			return null;
		} else {
			input.beginObject();
			
			JsonReader la = input.lookAhead();
			while (la.hasNext()) {
				String name = la.nextName();
				if (name.equals("@entity")) {
					entity = la.nextString();
				} else if (name.equals("id")) {
					id = idDeser.deserialize(context.subForInput(la), null, null);
				} else {
					la.skipValue();
				}
				// TODO if we can take for granted that the order is always id->@entity we could stop before, cause entities may not have an id
				if (entity != null && id != null) break;
			}
			la.close();
		}
		
		Class<?> clazz = null;
		// If we have an @entity, try to resolve it
		if (entity != null) {
			EntityNameProvider provider = context.getMapper().getEntityNameProvider();
			if (provider != null) {
				clazz = provider.getEntityClass(entity);
			}
			if (clazz == null) {
				try {
					clazz = Class.forName(entity);
				} catch (Exception e) {}
			}
			//if (clazz == null) throw new IllegalStateException("Cannot resolve @entity:" + entity);
		}
		
		// Try reusing pre
		if (clazz == null && pre != null) {
			clazz = pre.getClass();
		}
		if (pre != null) {
			if (factory != null) {
				Object preid = factory.getId(pre, context);
				if (preid != null) {
					if (context.getFromStackBoolean(REUSE_WITHOUT_ID)) {
						if (id == null || !preid.equals(id)) {
							pre = null;
						}
					} else { 
						if (id != null && !preid.equals(id)) {
							pre = null;
						}
					}
				} else if (id != null) {
					pre = null;
				}
			}
		}
		
		// If we don't have it, try based on the hint
		if (hint != null) {
			if (clazz == null) {
				if (!hint.isInstantiatable()) {
					if (entity == null)
						throw new IllegalStateException("No @entity in the json, and " + hint + " is not instantiatable");
					throw new IllegalStateException("Entity " + entity + " not mapped in factory, and " + hint + " is not instantiatable");
				}
				clazz = hint.getConcrete();
			} else {
				if (hint.hasConcrete()) {
					if (!hint.getConcrete().isAssignableFrom(clazz)) throw new IllegalStateException("Was expecting " + hint + " or subclass, but based on @entity it's a " + clazz.getName());
				}
			}
		}
		
		// Now we should have a class to work on

		// Check if the pre is coherent
		if (pre != null) {
			if (!clazz.isAssignableFrom(pre.getClass())) {
				pre = null;
			}
		}
		if (pre == null && clazz != null && factory != null) {
			if (id == null && !context.isRoot() && !context.getFromStackBoolean(ALLOW_NEW)) {
				throw new IllegalStateException("Cannot create new values here and no id provided");
			}
			pre = factory.buildEntity(clazz, id, context);
		}
		if (embedded) {
			if (pre == null) throw new IllegalStateException("Cannot deserialize embedded object " + id + " " + hint);
			return pre;
		}
		// Check if we can proceed modifying existing instance
		if (pre != null && factory != null && !context.isRoot() && !context.getFromStackBoolean(ALLOW_CHANGES)) {
			// Not allowed to apply changes, swallow the rest of the json
			while (input.hasNext()) {
				input.nextName();
				input.skipValue();
			}
			input.endObject();
			return pre;
		}
		// Check if we can proceed creating a new instance
        if (pre == null && !context.isRoot() && !context.getFromStackBoolean(ALLOW_NEW)) {
            throw new IllegalStateException("Cannot create new values here and factory didn't return any");
        }
		if (pre == null) {
			// Try to instantiate it
			pre = TypeUtil.get(clazz).newInstance();
		}
		
		// Now we should have a pre to work on
		JsonClassData cd = context.getMapper().getClassDataFactory().getClassData(pre.getClass(), context);
		if (factory != null) {
			pre = factory.prepare(pre, false, context);
		}
		
		if (id != null) {
			Map<String,Object> dones = (Map<String, Object>) context.get(CTX_BEAN_JSON_DE_SER_DONES);
			if (dones == null) {
				dones = new HashMap<>();
				context.put(CTX_BEAN_JSON_DE_SER_DONES, dones);
			}
			dones.put(id.toString(), pre);
		}
		while (input.hasNext()) {
			String name = input.nextName();
			context.deserializationEntering(name);
			context.putLocalStack(cd.getOptions(name));			
			Object preval = null;
			TypeUtil hintval = cd.getSetHint(name);
			if (hintval == null) hintval = cd.getGetHint(name);
			// TODO this is not right, we don't know if a custom deserializer will need or not the previous value
			if (hintval == null || !hintval.hasConcrete() || (!hintval.isCharSequence() && !hintval.isNumber())) {
				preval = cd.getValue(name, pre);
			}
			try {
				Object nval = context.getMapper().readValue(context, preval, hintval);
				cd.setValue(name, nval, pre);
			} finally {
				context.deserializationExited();
			}
		}
		input.endObject();

		if (factory != null) {
			pre = factory.finish(pre, false, context);
		}		

		return pre;
	}

}
