package net.etalia.jalia;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.etalia.jalia.annotations.JsonAllowEntityPropertyChanges;
import net.etalia.jalia.annotations.JsonAllowNewInstances;
import net.etalia.jalia.annotations.JsonCollection;
import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonGetter;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonIgnoreProperties;
import net.etalia.jalia.annotations.JsonInclude;
import net.etalia.jalia.annotations.JsonInclude.Include;
import net.etalia.jalia.annotations.JsonMap;
import net.etalia.jalia.annotations.JsonOnDemandOnly;
import net.etalia.jalia.annotations.JsonRequireIdForReuse;
import net.etalia.jalia.annotations.JsonSetter;
import net.etalia.utils.MissHolder;

public class JsonClassData {

	protected static final Set<String> ALL_SET = new HashSet<>();
	
	static {
		ALL_SET.add("*");
	}
	
	protected Class<?> clazz;

	/**
	 * Name of the fields that must be serialized by default, if no specific fields are requested.
	 */
	protected Set<String> defaults = new HashSet<>();

	/**
	 * Visible, not ignored, getters on this class.
	 */
	protected Map<String,Method> getters = new HashMap<>();

	/**
	 * Visible, not ignored, getters on this class, that must be serialized only if requested.
	 */
	protected Map<String,Method> ondemand = new HashMap<>();

	/**
	 * Visible, not ignored, setters on this class.
	 */
	protected Map<String,Method> setters = new HashMap<>();

	/**
	 * All getters on this class, including ignored ones.
	 */
	protected Map<String,Method> allGetters = new HashMap<>();

	/**
	 * All setters on this class, including ignored ones.
	 */
	protected Map<String,Method> allSetters = new HashMap<>();

	/**
	 * Options for each property, these are built parsing some annotations like for example {@link JsonCollection}.
	 */
	protected Map<String,Map<String,Object>> options = new HashMap<String, Map<String,Object>>();
	
	// Caches
	protected Map<String,MissHolder<TypeUtil>> getHints = new HashMap<>();
	protected Map<String,MissHolder<TypeUtil>> setHints = new HashMap<>();
	
	protected boolean isNew = true;
	
	protected JsonClassData(JsonClassData other) {
		clazz = other.clazz;
		defaults.addAll(other.defaults);
		getters.putAll(other.getters);
		setters.putAll(other.setters);
		ondemand.putAll(other.ondemand);
		allGetters.putAll(other.allGetters);
		allSetters.putAll(other.allSetters);
		options.putAll(other.options);
	}
	
	protected JsonClassData(Class<?> clazz) {
		this.clazz = clazz;
		parse(clazz);
	}

	/**
	 * @return true if this JsonClassData is new, that is, the unsetNew method has not yet been called.
	 */
	public boolean isNew() {
		return isNew;
	}

	/**
	 * Set this JsonClassData as not new anymore.
	 */
	public void unsetNew() {
		isNew = false;
	}

	/**
	 * Parse a class, anlyze annotation and method found in the class, and configure this JsonClassData accordingly.
	 * @param c The class to parse.
	 */
	private void parse(Class<?> c) {
		
		Set<String> ignore = new HashSet<String>();
		// Parse JsonIgnoreProperties
		{
			JsonIgnoreProperties ignoreAnn = c.getAnnotation(JsonIgnoreProperties.class);
			if (ignoreAnn != null) {
				ignore.addAll(Arrays.asList(ignoreAnn.value()));
			}
		}
		// Parse DefaultFieldsSerialization
		{
			JsonDefaultFields defaultfields = c.getAnnotation(JsonDefaultFields.class);
			if (defaultfields != null) {
				defaults.addAll(Arrays.asList(defaultfields.value().split(",")));
			}
		}
		// TODO parse JsonTypeInfo
		// TODO parse JsonSubTypes
		Method[] methods = c.getDeclaredMethods();
		// Parse annotated ones first, so that they get priority
		for (Method method : methods) {
			if (method.isAnnotationPresent(JsonGetter.class)) {
				parseGetter(method, ignore);
			}
			if (method.isAnnotationPresent(JsonSetter.class)) {
				parseSetter(method, ignore);
			}
		}
		// Parse not annotated after
		for (Method method : methods) {
			if (!Modifier.isPublic(method.getModifiers())) continue;
			if (method.getName().startsWith("get") || 
					(method.getName().startsWith("is") && 
							(method.getReturnType().equals(Boolean.class) || 
							method.getReturnType().equals(Boolean.TYPE))
				)) {
				parseGetter(method, ignore);
			}
			if (method.getName().startsWith("set")) {
				parseSetter(method, ignore);
			}
		}
		
		Class<?> sup = c.getSuperclass();
		if (sup != null) parse(sup);
		Class<?>[] interfaces = c.getInterfaces();
		for (Class<?> inter : interfaces) parse(inter);
		
		// Parse class annotations to fetch options and pass those to all getters
		Map<String, Object> globs = new HashMap<String, Object>();
		parseOptions(clazz, globs);
		if (globs.size() > 0) {
			for (Entry<String, Map<String, Object>> entry : options.entrySet()) {
				Map<String, Object> mopts = entry.getValue();
				if (mopts == null) {
					entry.setValue(new HashMap<String, Object>(globs));
				} else {
					HashMap<String, Object> nopts = new HashMap<String, Object>(globs);
					nopts.putAll(mopts);
					entry.setValue(nopts);
				}
			}
		}
	}

	/**
	 * Extract the property name from the method name.
	 *
	 * This method strips initial "get", "set" or "is", decapitalizes the name, or uses the name
	 * from {@link JsonGetter} or {@link JsonSetter} annotations.
	 *
	 * It also checks for ignored methods annotated with {@link JsonIgnore} and add the property name
	 * to the given "ignore" set.
	 *
	 * If the property has to be ignored, the name is returned prefixed with an exclamation mark.
	 *
	 * @param method The method to extract the name from.
	 * @param ignore The set of property names to ignore, new found properties with @JsonIgnore will be added to this
	 *                  set.
	 * @return The extracted normalized property name, prefixed with an exclamation mark if it has to be ignored.
	 */
	private String methodName(Method method, Set<String> ignore) {
		String name = null;
		boolean explicitSet = false;
		{
			JsonGetter annotation = method.getAnnotation(JsonGetter.class);
			if (annotation != null) {
				explicitSet = true;
				name = annotation.value();
			}
		}
		{
			JsonSetter annotation = method.getAnnotation(JsonSetter.class);
			if (annotation != null) {
				explicitSet = true;
				name = annotation.value();
			}
		}
		JsonIgnore ignoreAnn = method.getAnnotation(JsonIgnore.class);
		if (name == null || name.length() == 0) {
			name = method.getName();
			if (name.startsWith("is")) {
				name = name.substring(2);
			} else {
				name = name.substring(3);
			}
			name = decapitalize(name);
			if (ignore.contains(name) && !explicitSet) {
				return "!" + name;
			}
		}
		if (ignoreAnn != null) {
			if (ignoreAnn.value()) {
				ignore.add(name);
			}
			return "!"+name;
		}		
		return name;
	}
	
    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
		char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

	/**
	 * Parse a getter and add it to this class.
	 *
	 * @param method The method to parse.
	 * @param ignore The known set of property names to ignore.
	 */
	private void parseGetter(Method method, Set<String> ignore) {
		// Skip getClass
		if (method.getName().equals("getClass")) return;
		if (method.getReturnType().equals(Void.class)) return;
		if (method.getParameterTypes().length > 0) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		
		// Get options
		String name = methodName(method, ignore);
		String baseName = name.startsWith("!") ? name.substring(1) : name;
		Map<String, Object> opts = options.get(baseName);
		if (opts == null) opts = new HashMap<String, Object>();
		parseOptions(method, opts);
		if (!opts.isEmpty()) {
			options.put(baseName, opts);
		} else {
			options.put(baseName, null);
		}
		
		method.setAccessible(true);
		name = methodName(method, ignore);
		if (name.startsWith("!")) {
			allGetters.put(baseName, method);
			// Check if to remove also the setter
			Method setter = setters.get(baseName);
			if (setter != null) {
				if (methodName(setter, ignore).startsWith("!")) {
					setters.remove(baseName);
				}
			}
			return;
		}
		allGetters.put(name, method);
		if (ondemand.containsKey(name)) {
			return;
		}
		JsonOnDemandOnly onDemandAnn = method.getAnnotation(JsonOnDemandOnly.class);
		if (onDemandAnn != null) {
			ondemand.put(name, method);
			getters.remove(name);
		} else {
			if (getters.containsKey(name)) {
				return;
			}
			getters.put(name, method);
		}
	}

	/**
	 * Parse annotations and build a map of {@link Option} and values based on
	 * this annotations.
	 *
	 * @param ele The element (method or class) to analyze for annotations.
	 * @param opts The options found.
	 */
	private void parseOptions(AnnotatedElement ele, Map<String, Object> opts) {
		JsonInclude includeAnn = ele.getAnnotation(JsonInclude.class);
		if (includeAnn != null) {
			Include include = includeAnn.value();
			if (include == Include.ALWAYS) {
				opts.put(DefaultOptions.INCLUDE_EMPTY.toString(), true);
				opts.put(DefaultOptions.INCLUDE_NULLS.toString(), true);
			} else if (include == Include.NOT_NULL) {
				opts.put(DefaultOptions.INCLUDE_NULLS.toString(), false);
				opts.put(DefaultOptions.INCLUDE_EMPTY.toString(), true);
			} else if (include == Include.NOT_EMPTY) {
				opts.put(DefaultOptions.INCLUDE_NULLS.toString(), false);
				opts.put(DefaultOptions.INCLUDE_EMPTY.toString(), false);
			}
		}
		
		if (ele.isAnnotationPresent(JsonRequireIdForReuse.class)) {
			opts.put(BeanJsonDeSer.REUSE_WITHOUT_ID, true);
		}
		
		if (ele.isAnnotationPresent(JsonCollection.class)) {
			JsonCollection ann = ele.getAnnotation(JsonCollection.class);
			opts.put(ListJsonDeSer.DROP, ann.drop());
			opts.put(ListJsonDeSer.CLEAR, ann.clear());
		}
		if (ele.isAnnotationPresent(JsonMap.class)) {
			JsonMap ann = ele.getAnnotation(JsonMap.class);
			opts.put(MapJsonDeSer.RETAIN, ann.retain());
			opts.put(MapJsonDeSer.DROP, ann.drop());
			opts.put(MapJsonDeSer.CLEAR, ann.clear());
		}
		if (ele.isAnnotationPresent(JsonAllowNewInstances.class)) {
			opts.put(BeanJsonDeSer.ALLOW_NEW, true);
		}
		if (ele.isAnnotationPresent(JsonAllowEntityPropertyChanges.class)) {
			opts.put(BeanJsonDeSer.ALLOW_CHANGES, true);
		}
	}

	/**
	 * Parse a setter and add it to this class data.
	 * @param method The setter method to parse.
	 * @param ignore The set of property names to ignore.
	 */
	private void parseSetter(Method method, Set<String> ignore) {
		if (method.getParameterTypes().length != 1) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		method.setAccessible(true);
		String name = methodName(method, ignore);
		String baseName = name.startsWith("!") ? name.substring(1) : name;
		Map<String, Object> opts = options.get(baseName);
		if (opts == null) opts = new HashMap<String, Object>();
		parseOptions(method, opts);
		if (!opts.isEmpty()) {
			options.put(baseName, opts);
		} else {
			options.put(baseName, null);
		}
		if (name.startsWith("!")) {
			allSetters.put(baseName, method);
			// Check if to remove also the setter
			Method getter = getters.get(baseName);
			if (getter != null) {
				if (methodName(getter, ignore).startsWith("!")) {
					getters.remove(baseName);
				}
			}
			return;
		}
		allSetters.put(name, method);
		if (setters.containsKey(name)) {
			return;
		}
		setters.put(name, method);
	}

	/**
	 * Get the value of a property from and entity.
	 * @param name The name of the property to read.
	 * @param obj The entity to read from.
	 * @return The value or null if the property is ignored or an error occurs.
	 */
	public Object getValue(String name, Object obj) {
		return getValue(name, obj, false);
	}

	/**
	 * Get the value of a property from and entity.
	 * @param name The name of the property to read.
	 * @param obj The entity to read from.
	 * @param force If true, it will force getting the value even if the property is ognired.
	 * @return The value or null if the property is ignored and force is false, or if the property is not found or
	 * an error occurs.
	 */
	public Object getValue(String name, Object obj, boolean force) {
		Method method = getters.get(name);
		if (method == null) {
			method = ondemand.get(name);
		}
		if (method == null && force) {
			method = allGetters.get(name);
		}
		// TODO log this?
		if (method == null) return null;
		try {
			return method.invoke(obj);
		} catch (Throwable e) {
			// TODO log this?
			return null;
		}
	}

	/**
	 * @return a set of all visible property names that can be read.
	 */
	public Set<String> getGettables() {
		return getters.keySet();
	}

	/**
	 * @return a sorted list of all visible property names that can be read.
	 */
	public List<String> getSortedGettables() {
		ArrayList<String> ret = new ArrayList<String>(getters.keySet());
		Collections.sort(ret);
		return ret;
	}

	public Set<String> getOnDemandGettables() {
		return ondemand.keySet();
	}

	/**
	 * @return a set of all visible property names that can be written.
	 */
	public Set<String> getSettables() {
		return setters.keySet();
	}

	/**
	 * @return a set of default properties to serialize, or only the value "*" if no defaults were specified.
	 */
	public Set<String> getDefaults() {
		return defaults.size() > 0 ? defaults : ALL_SET;
	}

	/**
	 * @return the Class being handled by this JsonClassData.
	 */
	public Class<?> getTargetClass() {
		return clazz;
	}

	/**
	 * Programmatically ignore a setter by property name.
	 * @param string the name of the property whose setter must be ignored.
	 */
	public void ignoreSetter(String string) {
		setters.remove(string);
	}

	/**
	 * Programmatically ignore a getter by property name.
	 * @param string the name of the property whose getter must be ignored.
	 */
	public void ignoreGetter(String string) {
		getters.remove(string);
	}

	/**
	 * Get the type hint for the setter of a property.
	 * @param name The name of the property.
	 * @return the type hint.
	 */
	public TypeUtil getSetHint(String name) {
		MissHolder<TypeUtil> found = setHints.get(name);
		if (found != null) return found.getVal();
		TypeUtil ret = null;
		if (setters.containsKey(name)) {
			ret = TypeUtil.get(setters.get(name).getGenericParameterTypes()[0]);
		}
		setHints.put(name, new MissHolder<>(ret));
		return ret;
	}

	/**
	 * Get the type hint for the getter of a property.
	 * @param name The name of the property.
	 * @return the type hint.
	 */
	public TypeUtil getGetHint(String name) {
		MissHolder<TypeUtil> found = getHints.get(name);
		if (found != null) return found.getVal();
		TypeUtil ret = null;
		if (getters.containsKey(name)) {
			ret = TypeUtil.get(getters.get(name).getGenericReturnType());
		} 
		getHints.put(name, new MissHolder<>(ret));
		return ret;
	}

	/**
	 * Set a value on an entity.
	 * @param name The name of the property to set.
	 * @param nval The value to set.
	 * @param tgt The target entity.
	 * @return true if setting the value was successful, false otherwise.
	 */
	public boolean setValue(String name, Object nval, Object tgt) {
		return setValue(name, nval, tgt, false);
	}

	/**
	 * Set a value on an entity, optionally forcing on ignored setters.
	 * @param name The name of the property to set.
	 * @param nval The value to set.
	 * @param tgt The target entity.
	 * @param force if true will force setting also on ignored properties.
	 * @return true if setting the value was successful, false otherwise.
	 */
	public boolean setValue(String name, Object nval, Object tgt, boolean force) {
		Method method = setters.get(name);
		if (method == null && force) {
			method = allSetters.get(name);
		}
		// TODO log this?
		if (method == null) return false;
		try {
			method.invoke(tgt, nval);
		} catch (Throwable e) {
			// TODO log this?
			return false;
		}
		return true;
	}

	/**
	 * Get options for a property.
	 * @param name The property name.
	 * @return de-serialization options for the property, if any.
	 */
	public Map<String,Object> getOptions(String name) {
		return options.get(name);
	}
}
