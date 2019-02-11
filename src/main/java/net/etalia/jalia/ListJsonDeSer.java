package net.etalia.jalia;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.etalia.jalia.annotations.JsonCollection;
import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;

/**
 * De-serializer for ant {@link Iterable} and native arrays.
 */
public class ListJsonDeSer implements JsonDeSer {

	public static final String DROP = "LIST_JSON_DESER_DROP";
	public static final String CLEAR = "LIST_JSON_DESER_CLEAR";

	@Override
	public int handlesSerialization(JsonContext context, Class<?> clazz) {
		if (Iterable.class.isAssignableFrom(clazz)) return 10;
		if (clazz.isArray()) return 10;
		return -1;
	}
	
	@Override
	public int handlesDeserialization(JsonContext context, TypeUtil hint) {
		if (hint != null) {
			if (hint.isArray()) return 10;
			try {
				if (Iterable.class.isAssignableFrom(hint.getConcrete())) return 10;
			} catch (Exception ignored) {}
		}
		try {
			if (context.getInput().peek() == JsonToken.BEGIN_ARRAY) return 10;
		} catch (Exception ignored) {}
		return -1;
	}

	@Override
	public void serialize(Object obj, JsonContext context) throws IOException {
		JsonWriter output = context.getOutput();
		
		if (context.hasInLocalStack(CTX_ALL_SERIALIZESTACK, obj)) {
			// TODO this avoid loops, but also break serialization, cause there is no id to send
			output.clearName();
			return;
		}		
		context.putLocalStack(CTX_ALL_SERIALIZESTACK, obj);
		
		if (obj.getClass().isArray()) {
			if (Array.getLength(obj) == 0 && !context.isRoot() && !context.getFromStackBoolean(DefaultOptions.INCLUDE_EMPTY.toString())) {
				output.clearName();
				return;
			}
			output.beginArray();
			for (int i = 0; i < Array.getLength(obj); i++) {
				context.getMapper().writeValue(Array.get(obj, i), context);
			}
		} else {
			if (!((Iterable)obj).iterator().hasNext() && !context.isRoot() && !context.getFromStackBoolean(DefaultOptions.INCLUDE_EMPTY.toString())) {
				output.clearName();
				return;
			}
			output.beginArray();
			for (Object so : (Iterable)obj) {
				context.getMapper().writeValue(so, context);
			}
		}
		output.endArray();
	}

	/**
	 * Deserializes a List, Set or array.
	 * <p>
	 * During deserialization, the following applies:
	 * <ul>
	 *     <li>If {@link DefaultOptions#RECORD_CHANGES} is active, the existing collection (if any) will be copied in
	 *     an ArrayList and used as original value in the recording.
	 *     <li>The existing collection will be reused, unless {@link JsonCollection#drop()} is set.
	 *     <li>Existing values inside the collection, if any, will be reused and updated with found JSON, as long as
	 *     there is a pre-existing collection to reuse, and at given index there is an object to reuse.
	 *     <li>If {@link JsonCollection#clear()} is specified, the collection is cleared, so no existing object will
	 *     be available for reuse.
	 *     <li>Elements not existing in the JSON (matched by index in the array) will be removed by the existing
	 *     collection.
	 *     <li>If the existing type is an array, and the input JSON array size is different, the existing array will not
	 *     be reused cause arrays in Java cannot be resized.
	 * </ul>
	 * @param context the current deserialization context
	 * @param pre the existing value to modify if any, null otherwise
	 * @param hint a hint ont he expected return type
	 * @return the deserialized collection
	 * @throws IOException if something goes wrong reading from the input JSON
	 */
	@Override
	public Object deserialize(JsonContext context, Object pre, TypeUtil hint) throws IOException {
		Collection<Object> act;
		boolean wasArray = (pre != null && pre.getClass().isArray()); 
		if (pre != null && pre.getClass().isArray()) {
			act = new ArrayList<Object>();
			for (int i = 0; i < Array.getLength(pre); i++) {
				act.add(Array.get(pre, i));
			}
		} else {
			act = (Collection<Object>) pre;
		}
		TypeUtil inner = null;
		if (act != null) {
			TypeUtil pretype = TypeUtil.get(pre.getClass());
			if (pretype.isArray()) {
				inner = pretype.getArrayType();
			} else {
				if (List.class.isAssignableFrom(pretype.getConcrete())) {
					inner = pretype.findReturnTypeOf("get", Integer.TYPE);
				} else if (Set.class.isAssignableFrom(pretype.getConcrete())) {
					inner = pretype.findParameterOf("add", 0);
				}
			}
		}

		Collection<Object> originalValue = null;
		if (context.getFromStackBoolean(DefaultOptions.RECORD_CHANGES.toString()) && act != null) {
			originalValue = new ArrayList<>(act);
			context.putLocalStack(CTX_ALL_ORIGINAL_VALUE, originalValue);
		}

		if (context.getFromStackBoolean(DROP) || inner == null || !inner.hasConcrete() || inner.getConcrete() == Object.class) {
			if (context.getFromStackBoolean(DROP)) act = null;
			if (hint != null) {
				if (hint.isInstantiatable()) {
					try {
						act = (List<Object>) hint.getConcrete().newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO log this
						e.printStackTrace();
					}
				}
				if (hint.isArray()) {
					inner = hint.getArrayType();
				} else {
					if (List.class.isAssignableFrom(hint.getConcrete())) {
						inner = hint.findReturnTypeOf("get", Integer.TYPE);
					} else if (Set.class.isAssignableFrom(hint.getConcrete())) {
						inner = hint.findParameterOf("add", 0);
					}
				}				
			}
			if (act == null) act = new ArrayList<Object>();
		}
		
		JsonReader input = context.getInput();
		input.beginArray();
		{
			int i = 0;
			List<Object> lst;
			if (act instanceof List) {
				lst = (List<Object>)act;
			} else {
				lst = new ArrayList<>(act);
			}
			if (context.getFromStackBoolean(CLEAR)) lst.clear();
			List<Object> found = new ArrayList<>();
			while (input.hasNext()) {
				Object preval = null;
				if (i < lst.size()) preval = lst.get(i);
				Object val = context.getMapper().readValue(context, preval, inner);
				found.add(val);
				try {
					if (act instanceof List) {
						while (i >= act.size()) act.add(null);
						((List<Object>)act).set(i, val);
					} else {
						act.add(val);
					}
				} catch (UnsupportedOperationException e) {
					// Could happen for unmodifiable collections
					if (act instanceof List) {
						act = new ArrayList<>();
						while (i >= act.size()) act.add(null);
						((List<Object>)act).set(i, val);
					} else {
						act = new HashSet<>();
						act.add(val);
					}
				}
				i++;
			}
			if (act instanceof List) {
				while (act.size() > i) ((List<Object>)act).remove(i);
			} else {
				act.retainAll(found);
			}
		}
		input.endArray();
		if (wasArray || (hint != null && hint.isArray())) {
			if (pre == null || Array.getLength(pre) != act.size()) {
				pre = Array.newInstance(inner.getConcrete(), act.size());
			}
			Iterator<Object> iter = act.iterator();
			for (int i = 0; i < Array.getLength(pre); i++) {
				Array.set(pre, i, iter.next());
			}
			return pre;
		}
		return act;
	}

}
