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

import net.etalia.jalia.annotations.JsonCollection;
import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonRequireIdForReuse;
import net.etalia.jalia.annotations.JsonGetter;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonIgnoreProperties;
import net.etalia.jalia.annotations.JsonInclude;
import net.etalia.jalia.annotations.JsonInclude.Include;
import net.etalia.jalia.annotations.JsonOnDemandOnly;
import net.etalia.jalia.annotations.JsonSetter;
import net.etalia.utils.MissHolder;

public class JsonClassData {

	protected static final Set<String> ALL_SET = new HashSet<>();
	
	static {
		ALL_SET.add("*");
	}
	
	protected Class<?> clazz;
	
	protected Set<String> defaults = new HashSet<>();
	
	protected Map<String,Method> getters = new HashMap<>();
	protected Map<String,Method> ondemand = new HashMap<>();
	protected Map<String,Method> setters = new HashMap<>();
	
	protected Map<String,Method> allGetters = new HashMap<>();
	protected Map<String,Method> allSetters = new HashMap<>();
	
	protected Map<String,Map<String,Object>> options = new HashMap<String, Map<String,Object>>();
	
	// Caches
	protected Map<String,MissHolder<TypeUtil>> getHints = new HashMap<>();
	protected Map<String,MissHolder<TypeUtil>> setHints = new HashMap<>();
	
	protected boolean isNew = true;
	
	protected JsonClassData(JsonClassData other) {
		this.clazz = other.clazz;
		this.defaults.addAll(other.defaults);
		this.getters.putAll(other.getters);
		this.setters.putAll(other.setters);
		this.ondemand.putAll(other.ondemand);
		this.allGetters.putAll(other.allGetters);
		this.allSetters.putAll(other.allSetters);
		this.options.putAll(other.options);
	}
	
	protected JsonClassData(Class<?> clazz) {
		this.clazz = clazz;
		parse(clazz);
	}
	
	public boolean isNew() {
		return isNew;
	}
	public void unsetNew() {
		this.isNew = false;
	}
	
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
				this.defaults.addAll(Arrays.asList(defaultfields.value().split(",")));
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
		
		// Add class configuration to all getters 
		Map<String, Object> globs = new HashMap<String, Object>();
		parseOptions(clazz, globs);
		if (globs.size() > 0) {
			for (Entry<String, Map<String, Object>> entry : this.options.entrySet()) {
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
			if (ignore.contains(name) && !explicitSet && (ignoreAnn == null || ignoreAnn.value())) return "!" + name;
		}
		if (ignoreAnn != null && ignoreAnn.value()) {
			ignore.add(name);
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
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
	
	
	private void parseGetter(Method method, Set<String> ignore) {
		// Skip getClass
		if (method.getName().equals("getClass")) return;
		if (method.getReturnType().equals(Void.class)) return;
		if (method.getParameterTypes().length > 0) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		
		// Get options
		String name = methodName(method, ignore);
		String baseName = name.startsWith("!") ? name.substring(1) : name;
		Map<String,Object> opts = this.options.get(baseName); 
		if (opts == null) opts = new HashMap<String, Object>();
		parseOptions(method, opts);
		if (!opts.isEmpty()) {
			this.options.put(baseName, opts);
		} else {
			this.options.put(baseName, null);
		}
		
		method.setAccessible(true);
		name = methodName(method, ignore);
		if (name.startsWith("!")) {
			allGetters.put(baseName, method);
			// Check if to remove also the setter
			Method setter = this.setters.get(baseName);
			if (setter != null) {
				if (methodName(setter, ignore).startsWith("!")) {
					this.setters.remove(baseName);
				}
			}
			return;
		}
		allGetters.put(name, method);
		if (this.ondemand.containsKey(name)) return;
		JsonOnDemandOnly onDemandAnn = method.getAnnotation(JsonOnDemandOnly.class);
		if (onDemandAnn != null) {
			this.ondemand.put(name, method);
			this.getters.remove(name);
		} else {
			if (this.getters.containsKey(name)) return;
			this.getters.put(name, method);
		}
	}
	
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
	}

	private void parseSetter(Method method, Set<String> ignore) {
		if (method.getParameterTypes().length != 1) return;
		if (Modifier.isStatic(method.getModifiers())) return;
		method.setAccessible(true);
		String name = methodName(method, ignore);
		String baseName = name.startsWith("!") ? name.substring(1) : name;
		if (name.startsWith("!")) {
			allSetters.put(baseName, method);
			// Check if to remove also the setter
			Method getter = this.getters.get(baseName);
			if (getter != null) {
				if (methodName(getter, ignore).startsWith("!")) {
					this.getters.remove(baseName);
				}
			}
			return;
		}
		allSetters.put(name, method);
		if (this.setters.containsKey(name)) return;
		this.setters.put(name, method);
	}

	public Object getValue(String name, Object obj) {
		return getValue(name, obj, false);
	}
	
	public Object getValue(String name, Object obj, boolean force) {
		Method method = this.getters.get(name);
		if (method == null) method = this.ondemand.get(name);
		if (method == null && force) method = this.allGetters.get(name);
		// TODO log this?
		if (method == null) return null;
		try {
			return method.invoke(obj);
		} catch (Throwable e) {
			// TODO log this?
			return null;
		}
	}

	public Set<String> getGettables() {
		return this.getters.keySet();
	}
	
	public List<String> getSortedGettables() {
		ArrayList<String> ret = new ArrayList<String>(this.getters.keySet());
		Collections.sort(ret);
		return ret;
	}

	public Set<String> getSettables() {
		return this.setters.keySet();
	}
	
	public Set<String> getDefaults() {
		return this.defaults.size() > 0 ? this.defaults : ALL_SET;
	}

	public Class<?> getTargetClass() {
		return this.clazz;
	}

	public void ignoreSetter(String string) {
		this.setters.remove(string);
	}
	
	public void ignoreGetter(String string) {
		this.getters.remove(string);
	}

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

	public boolean setValue(String name, Object nval, Object tgt) {
		return setValue(name, nval, tgt, false);
	}	
	
	public boolean setValue(String name, Object nval, Object tgt, boolean force) {
		Method method = this.setters.get(name);
		if (method == null && force) method = this.allSetters.get(name);
		// TODO log this?
		if (method == null) return false;
		try {
			method.invoke(tgt, nval);
		} catch (Throwable e) {
			// TODO log this?
			return false;
		}
		return false;
	}

	public Map<String,Object> getOptions(String name) {
		return this.options.get(name);
	}
}
