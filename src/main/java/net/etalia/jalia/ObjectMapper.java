package net.etalia.jalia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;
import net.etalia.jalia.stream.JsonWriter;
import net.etalia.jalia.stream.MalformedJsonException;
import net.etalia.utils.LockHashMap;
import net.etalia.utils.MissHolder;

/**
 * Main class to access Jalia functionalities, implementing an interface similar to Jackson ObjectMapper.
 */
public class ObjectMapper {

	/**
	 * Cache of registered serializer and deserializers.
	 */
	private final List<JsonDeSer> registeredDeSers = new ArrayList<>();

	/**
	 * De-serializer used for null values.
	 */
	private JsonDeSer nullDeSer = null;

	/**
	 * De-serializer used for natives (byte, int, long, String etc..)
	 */
	private NativeJsonDeSer nativeDeSer = null;

	/**
	 * Cached class to Serializer map.
	 */
	private final LockHashMap<Class<?>,MissHolder<JsonDeSer>> serializers = new LockHashMap<>();

	/**
	 * Cached class (actually, TypeUtil) to Deserializer map.
	 */
	private final LockHashMap<TypeUtil,MissHolder<JsonDeSer>> deserializers = new LockHashMap<>();

	/**
	 * Entity factory used in this ObjectMapper.
	 */
	private EntityFactory entityProvider = null;

	/**
	 * Entity name provider used in this ObjectMapper.
	 */
	private EntityNameProvider entityNameProvider = null;

	/**
	 * Class data factory used in this ObjectMapper.
	 */
	private JsonClassDataFactory classDataFactory = new JsonClassDataFactoryImpl();

	/**
	 * Default serialization and deserializtion options.
	 */
	private final Map<String,Object> defaultOptions = new HashMap<String, Object>();
	
	{
		defaultOptions.put(DefaultOptions.PRETTY_PRINT.toString(), false);
		defaultOptions.put(DefaultOptions.INCLUDE_EMPTY.toString(), false);
		defaultOptions.put(DefaultOptions.INCLUDE_NULLS.toString(), false);
	}

	/**
	 * Flag to indicate whether this ObjectMapper has been already initialized. @see #init().
	 */
	protected boolean inited = false;

	/**
	 * Set a serialization/deserialization option. See {@link DefaultOptions} for the default ones,
	 * or extend {@link Option} to provide your own to work with your custom de-serializers and factories.
	 *
	 * @param option the option to set
	 * @param val The value for the option
	 * @param <X> The type of the option value
	 * @return this same instance to support fluent calls.
	 */
	public <X> ObjectMapper setOption(Option<X> option, X val) {
		defaultOptions.put(option.toString(), val);
		return this;
	}

	/**
	 * Set which Entity factory to use for this ObjectMapper.
	 *
	 * @param entityFactory The entity factory to use in this ObjectMapper.
	 * @return this same instance to support fluent calls.
	 */
	// TODO support more than one factory or cascading factories, and/or a factory that self configures based on typeinfo annotations
	public ObjectMapper setEntityFactory(EntityFactory entityFactory) {
		this.entityProvider = entityFactory;
		return this;
	}

	/**
	 * @return the entity factory currently used by this ObjectMapper.
	 */
	public EntityFactory getEntityFactory() {
		return entityProvider;
	}

	/**
	 * Set which Entity name provider to use for this ObjectMapper.
	 *
	 * @param entityNameProvider The entity name provider to use.
	 * @return this same instance to support fluent calls.
	 */
	public ObjectMapper setEntityNameProvider(EntityNameProvider entityNameProvider) {
		this.entityNameProvider = entityNameProvider;
		return this;
	}

	/**
	 * @return the entity name provider used by this ObjectMapper.
	 */
	public EntityNameProvider getEntityNameProvider() {
		return entityNameProvider;
	}

	/**
	 * Set which JSON class data factory to use for this ObjectMapper.
	 *
	 * @param classDataFactory The json class data factory to use.
	 * @return this same instance to support fluent calls.
	 */
	public ObjectMapper setClassDataFactory(JsonClassDataFactory classDataFactory) {
		this.classDataFactory = classDataFactory;
		return this;
	}

	/**
	 * @return The json class data factory in use by this ObjectMapper.
	 */
	public JsonClassDataFactory getClassDataFactory() {
		return classDataFactory;
	}

	/**
	 * Programmatically register a de-serializer on this ObjectMapper.
	 * @param ds the de-serializer to register.
	 */
	public void registerDeSer(JsonDeSer ds) {
		registeredDeSers.add(ds);
	}

	/**
	 * Programmatically register multiple de-serializers on this ObjectMapper.
	 * @param dss the de-serializers to register.
	 */
	public void registerDeSer(Collection<? extends JsonDeSer> dss) {
		registeredDeSers.addAll(dss);
	}

	/**
	 * @return The de-serializers registered on this ObjectMapper.
	 */
	public List<JsonDeSer> getRegisteredDeSers() {
		return registeredDeSers;
	}

	/**
	 * Initialize this ObjectMapper with defaults, adding simple de-serializers.
	 */
	@PostConstruct
	public void init() {
		if (inited) return;
		inited = true;
		NativeJsonDeSer nativeDeSer = new NativeJsonDeSer();
		if (nullDeSer == null) nullDeSer = nativeDeSer;
		if (this.nativeDeSer == null) this.nativeDeSer = nativeDeSer; 
		registeredDeSers.add(nativeDeSer);
		registeredDeSers.add(new MapJsonDeSer());
		registeredDeSers.add(new ListJsonDeSer());
		registeredDeSers.add(new BeanJsonDeSer());
	}

	/**
	 * Configure a reader before the ObjectMapper starts reading from it. This can be overridden to provide
	 * customized setup of readers.
	 * @param reader The reader to configure.
	 * @return the same or a new instance of {@link JsonReader}.
	 */
	protected JsonReader configureReader(JsonReader reader) {
		return reader;
	}

	/**
	 * Configure a writer before the ObjectMapper starts writing to it. This can be overridden to provide
	 * customized setup of writers.
	 * @param writer The writer to configure.
	 * @return the same or a new instance of {@link JsonWriter}.
	 */
	protected JsonWriter configureWriter(JsonWriter writer) {
		if ((Boolean)defaultOptions.get(DefaultOptions.PRETTY_PRINT.toString()))
			writer.setIndent("  ");
		return writer;
	}

	/**
	 * Search for a serializer for an object in the current context.
	 *
	 * Selection is based on calling {@link JsonDeSer#handlesSerialization(JsonContext, Class)} on all registered
	 * serializers in order, an then finding the one that scored best or the first one that scored more than or equal to
	 * 10.
	 *
	 * @param context The current serialization context.
	 * @param obj The entity to be serialized.
	 * @return The most appropriate serialized among the registered ones.
	 */
	protected JsonDeSer getSerializerFor(JsonContext context, Object obj) {
		Class<?> clazz;
		if (obj == null) return nullDeSer;
		clazz = obj.getClass();

		MissHolder<JsonDeSer> holder;
		serializers.lockRead();
		try {
			holder = serializers.get(clazz);
		} finally {
			serializers.unlockRead();
		}
		if (holder != null) return holder.getVal();
		
		JsonDeSer deser = null;
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesSerialization(context, clazz);
				if (ach > max) {
					deser = acds;
					max = ach;
					if (max >= 10) break;
				}
			} catch (NullPointerException ignored) {}
		}
		serializers.lockWrite();
		try {
			serializers.put(clazz, new MissHolder<>(deser));
		} finally {
			serializers.unlockWrite();
		}
		
		return deser;
	}

	/**
	 * Search for a deserializer for an object in the current context.
	 *
	 * Selection is based on calling {@link JsonDeSer#handlesDeserialization(JsonContext, Class)} on all registered
	 * deserializers in order, an then finding the one that scored best or the first one that scored more than or equal
	 * to 10.
	 *
	 * @param context The current deserialization context.
	 * @param hint A hint on the expected type.
	 * @param useCache Whether or not to use the cached values.
	 * @return The most appropriate deserialized among the registered ones.
	 */
	protected JsonDeSer getDeserializerFor(JsonContext context, TypeUtil hint, boolean useCache) {
		if (hint != null && useCache) {
			MissHolder<JsonDeSer> holder;
			deserializers.lockRead();
			try {
				holder = deserializers.get(hint);
			} finally {
				deserializers.unlockRead();
			}
			if (holder != null) return holder.getVal();
		}
		JsonDeSer deser = null;
		
		int max = -1;
		for (JsonDeSer acds : registeredDeSers) {
			try {
				int ach = acds.handlesDeserialization(context, hint);
				if (ach > max) {
					deser = acds;
					max = ach;
					if (max >= 10) break;
				}
			} catch (NullPointerException ignored) {}
		}
		return deser;
	}

	/**
	 * Caches a deserializer for the given type hint.
	 * @param hint The type hint.
	 * @param deser The deserializer.
	 */
	protected void cacheDeserializerFor(TypeUtil hint, JsonDeSer deser) {
		if (hint != null) {
			deserializers.lockWrite();
			try {
				deserializers.put(hint, new MissHolder<>(deser));
			} finally {
				deserializers.unlockWrite();
			}
		}
	}

	/**
	 * Invalidates the deserializers cache for a given type hint.
	 * @param hint The type hint.
	 */
	protected void invalidateDeserializerCache(TypeUtil hint) {
		if (hint != null) {
			deserializers.lockWrite();
			try {
				deserializers.remove(hint);
			} finally {
				deserializers.unlockWrite();
			}
		}
	}
	
	public void writeValue(JsonWriter jsonOut, OutField fields, Object obj) {
		init();
		configureWriter(jsonOut);
		JsonContext ctx = createContext();
		ctx.initInheritStack(this.defaultOptions);
		ctx.setOutput(jsonOut);
		if (fields == null) fields = new OutField(true);
		ctx.setRootFields(fields);
		writeValue(obj, ctx);
	}
	
	public void writeValue(Object obj, JsonContext context) {
		JsonDeSer deser = getSerializerFor(context, obj);
		if (deser == null) throw new JaliaException("Cannot find a JSON serializer for " + obj + " at " + context.getStateLog());
		try {
			deser.serialize(obj, context);
		} catch (Throwable t) {
			throw new JaliaException("Error writing " + context.getStateLog(), t);
		}
	}
	
	
	public Object readValue(JsonReader jsonIn, Object pre, TypeUtil hint) {
		init();
		configureReader(jsonIn);
		JsonContext ctx = createContext();
		ctx.initInheritStack(this.defaultOptions);
		ctx.setInput(jsonIn);
		boolean valid;
		try {
			JsonToken prepeek = jsonIn.peek();
			valid = 
				(prepeek == JsonToken.BEGIN_ARRAY)
				||
				(prepeek == JsonToken.BEGIN_OBJECT);
		} catch (MalformedJsonException mje) {
			valid = false;
		} catch (IOException e) {
			throw new JaliaException("Error reading input stream", e);
		}
		if (!valid) {
			jsonIn.setLenient(true);
			try {
				return nativeDeSer.deserialize(ctx, pre, hint);
			} catch (Exception e) {
				throw new JaliaException("Error parsing raw value", e);
			}
		}
		try {
			return readValue(ctx, pre, hint);
		} catch (Exception e) {
			if (e instanceof JaliaException) throw e;
			throw new JaliaException("Error parsing " + ctx.getStateLog(), e);
		}
	}
	
	public Object readValue(JsonContext ctx, Object pre, TypeUtil hint) {
		// Don't consider a hint == Object.class
		if (hint != null && hint.getType().equals(Object.class)) hint = null;

		// TODO: why is this always false?
		JsonDeSer deser = getDeserializerFor(ctx, hint, false);
		if (deser == null) throw new JaliaException("Cannot find a JSON deserializer for " + pre + " " + hint + " at " + ctx.getStateLog());
		try {
			Object ret = deser.deserialize(ctx, pre, hint);
			this.cacheDeserializerFor(hint, deser);
			return ret;
		} catch (Throwable t) {
			if (t instanceof JaliaException) throw (JaliaException)t;
			throw new JaliaException("Error reading " + ctx.getStateLog(), t);
		}
	}
	
	protected JsonContext createContext() {
		return new JsonContext(this);
	}
	
	// ---- Utility methods

	public void writeValue(Writer out, OutField fields, Object obj) {
		init();
		if (obj == null || nativeDeSer.handlesSerialization(null, obj.getClass()) == 10) {
			try {
				nativeDeSer.serializeRaw(obj, out);
			} catch (IOException e) {
				throw new IllegalStateException("Error while raw serializing", e);
			}
		} else {
			JsonWriter jw = new JsonWriter(out);
			writeValue(jw, fields, obj);
		}
	}

	public void writeValue(Writer out, Object obj) {
		writeValue(out, null, obj);
	}	
	
	public void writeValue(OutputStream out, OutField fields, Object obj) {
		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(out, Charset.forName("UTF-8"));
			writeValue(osw, fields, obj);
		} finally {
			try {
				osw.close();
			} catch (Exception ignored) {}
		}
	}
	
	public String writeValueAsString(Object obj, OutField fields) {
		StringWriter writer = new StringWriter();
		writeValue(writer, fields, obj);
		return writer.toString();
	}
	
	public String writeValueAsString(Object obj) {
		return writeValueAsString(obj, null);
	}	
	
	public void writeValue(OutputStream stream, Object obj) {
		writeValue(stream, null, obj);
	}

	public byte[] writeValueAsBytes(Object obj, OutField fields) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		//BufferedOutputStream bos = new BufferedOutputStream(baos);
		try {
			writeValue(baos, fields, obj);
		} finally {
			try {
				//bos.flush();
				baos.close();
			} catch (IOException ignored) {}
		}
		return baos.toByteArray();
	}
	
	
	public byte[] writeValueAsBytes(Object obj) {
		return writeValueAsBytes(obj, null);
	}
	
	
	
	public <T> T readValue(InputStream in, TypeUtil hint) {
		return readValue(in, null, hint);
	}
	
	public <T> T readValue(InputStream in, T pre, TypeUtil hint) {
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(in, Charset.forName("UTF-8"));
			return readValue(isr, pre, hint);
		} finally {
			try {
				isr.close();
			} catch (Exception ignored) {}
		}
	}	

	public <T> T readValue(InputStream in, Class<T> clazz) {
		return readValue(in, null, clazz);
	}
	
	public <T> T readValue(InputStream in, T pre, Class<T> clazz) {
		return readValue(in, TypeUtil.get(clazz));
	}
	
	public <T> T readValue(Reader r, TypeUtil hint) {
		return readValue(r, null, hint);
	}
	
	public <T> T readValue(Reader r, T pre, TypeUtil hint) {
		// Special case when we know we expect a string
		if (hint != null && hint.isCharSequence()) {
			StringWriter sw = new StringWriter();
			char[] buff = new char[1024];
			int len;
			try {
				while ((len = r.read(buff)) >= 0) {
					sw.write(buff, 0, len);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Error reading from stream", e);
			}
			return (T)sw.toString();
		}
		JsonReader reader = new JsonReader(r);
		return (T)readValue(reader, pre, hint);
	}

	public <T> T readValue(String json, TypeUtil hint) {
		return readValue(json, null, hint);
	}
	
	public <T> T readValue(String json, T pre, TypeUtil hint) {
		StringReader reader = new StringReader(json);
		return readValue(reader, pre, hint);
	}

	public <T> T readValue(String json, Class<T> clazz) {
		return readValue(json, null, clazz);
	}
	
	public <T> T readValue(String json, T pre, Class<T> clazz) {
		return readValue(json, pre, TypeUtil.get(clazz));
	}	

	public <T> T readValue(byte[] json, TypeUtil hint) {
		return readValue(json, null, hint);
	}
	
	public <T> T readValue(byte[] json, T pre, TypeUtil hint) {
		ByteArrayInputStream bais = new ByteArrayInputStream(json);
		try {
			return readValue(bais, pre, hint);
		} finally {
			try {
				bais.close();
			} catch (IOException ignored) {}
		}
	}

	public <T> T readValue(byte[] json, Class<T> clazz) {
		return readValue(json, null, clazz);
	}
	
	public <T> T readValue(byte[] json, T pre, Class<T> clazz) {
		return readValue(json, pre, TypeUtil.get(clazz));
	}


	public <T> T readValue(byte[] json) {
		return readValue(json, null, (TypeUtil)null);
	}	
	public <T> T readValue(byte[] json, T pre) {
		return readValue(json, pre, pre == null ? null : TypeUtil.get(pre.getClass()));
	}	
	public <T> T readValue(String json) {
		return readValue(json, (TypeUtil)null);
	}
	public <T> T readValue(String json, T pre) {
		return readValue(json, pre, pre == null ? null : TypeUtil.get(pre.getClass()));
	}
}
