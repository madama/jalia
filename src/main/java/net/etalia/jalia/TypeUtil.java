package net.etalia.jalia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.etalia.utils.MissHolder;

/**
 * Represents a "type" offering a number of utilities.
 * <p>
 * It can be instantiated in two ways. For simple types or types for already defined classes, fields or methods:
 * <code>
 *     TypeUtil.get(_java_type_);
 * </code>
 * Instead for inline types, {@link Specific} can be used as follows:
 * <code>
 *     new TypeUtil.specific&lt;List&lt;String&gt;&gt;() {}.type();
 * </code>
 * Note the "{}" above, cause we are actually creating an anonymous class from where to extract the type.
 */
public class TypeUtil {

	/**
	 * Cache of already known type.
	 */
	private static final ConcurrentMap<Type, TypeUtil> cache = new ConcurrentHashMap<Type, TypeUtil>();

	/**
	 * Get the TypeUtil for a {@link Type}, which can be a {@link Class} or a any other Type subinterface,
	 *
	 * @param type the type to analyze
	 * @return the corresponding TypeUtil instance
	 */
	public static TypeUtil get(Type type) {
		if (type == null) return null;
		TypeUtil ret = cache.get(type);
		if (ret != null) 
			return ret;
		ret = new TypeUtil(type);
		TypeUtil pre = cache.putIfAbsent(type, ret);
		if (pre != null) 
			ret = pre;
		return ret;
	}

	/**
	 * The type this instance if handling.
	 */
	private final Type type;
	
	// Caches
	/**
	 * Cached concrete Class, if applicable.
	 */
	private Class<?> concrete;

	/**
	 * Cached method return types.
	 */
	private final Map<String,MissHolder<TypeUtil>> returnTypes = new HashMap<>();

	/**
	 * Cached enum values, if this TypeUtil represents an Enum.
	 */
	private Enum<?>[] enums;

	/**
	 * Cached value, true if the type this instance is handling has a concrete type.
	 */
	private Boolean hasConcreteCache;

	/**
	 * Cached value, true if the type this instance if handling is instantiatable.
	 */
	private Boolean isInstantiatableCache; 
	
	private TypeUtil(Type type) {
		this.type = type;
	}

	/**
	 * @return the underlying Type handled by this instance
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Tries to extrapolate the concrete (plain Class) type.
	 *
	 * @return the concrete type
	 * @throws IllegalArgumentException if concrete type cannot be extrapolated
	 */
	public Class<?> getConcrete() {
		if (concrete != null) return concrete;
		if (type instanceof Class) {
			concrete = (Class<?>)type;
		} else if (type instanceof ParameterizedType) {
			concrete = (Class<?>)((ParameterizedType) type).getRawType();
		} else if (type instanceof WildcardType) {
			concrete = (Class<?>)((WildcardType) type).getUpperBounds()[0];
		} else throw new IllegalArgumentException("Can't parse type " + type);		
		return concrete;
	}

	/**
	 * @return true if there is a concrete (plain Class) extrapolable
	 */
	public boolean hasConcrete() {
		if (hasConcreteCache != null) return hasConcreteCache;
		try {
			getConcrete();
			hasConcreteCache = true;
		} catch (Exception e) {
			hasConcreteCache = false;
		}
		return hasConcreteCache;
	}

	/**
	 * Internal implementation that tried to resolve a TypeVariable in the context of this type.
	 * @param type the type variable to resolve
	 * @return the Type of the TypeVariable or null if it cannot be determined
	 */
	private Type resolveType(Type type) {
		if (!(type instanceof TypeVariable)) return type;
		TypeUtil ptype = this;
		while (ptype != null && !(ptype.type instanceof ParameterizedType)) {
			ptype = get(ptype.getConcrete().getGenericSuperclass());
		}
		if (ptype == null) return null; 
		TypeVariable var = (TypeVariable) type;
		Class<?> conc = ptype.getConcrete();
		TypeVariable<?>[] params = conc.getTypeParameters();
		int ind = -1;
		for (int i = 0; i < params.length; i++) {
			if (params[i].equals(var)) {
				ind = i;
				break;
			}
		}
		if (ind == -1) return null;
		return ((ParameterizedType)ptype.type).getActualTypeArguments()[ind];
	}

	/**
	 * Tries to resolve the return type of a method.
	 *
	 * @param methodName name of the method
	 * @param params parameters of the method (to distinguish overrides)
	 * @return the TypeUtil, or null if type cannot be resolved
	 */
	public TypeUtil findReturnTypeOf(String methodName, Class<?>... params) {
		String key = "r-" + methodName + (params == null ? "0" : Arrays.toString(params));
		MissHolder<TypeUtil> found = returnTypes.get(key);
		if (found != null) return found.getVal();
		Method method;
		try {
			method = getConcrete().getMethod(methodName, params);
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO log this
			return null;
		}
		Type retType = resolveType(method.getGenericReturnType());
		TypeUtil ret = null;
		if (retType == null) {
			returnTypes.put(key, new MissHolder<TypeUtil>(null));
		} else {
			ret = get(retType);
			returnTypes.put(key, new MissHolder<TypeUtil>(ret));
		}
		return ret;
	}

	/**
	 * Tries to resolve the type of a parameter or a method
	 * @param methodName method name
	 * @param paramIndex index (0 based) of the parameter
	 * @return the TypeUtil, or null if type cannot be resolved
	 */
	public TypeUtil findParameterOf(String methodName, int paramIndex) {
		String key = "p-" + paramIndex + methodName;
		MissHolder<TypeUtil> found = returnTypes.get(key);
		if (found != null) return found.getVal();
		TypeUtil ret = null;
		try {
			Method[] methods = getConcrete().getMethods();
			for (Method m : methods) {
				if (!m.getName().equals(methodName)) continue;
				Type[] params = m.getGenericParameterTypes();
				if (params.length <= paramIndex) continue;
				ret = get(resolveType(params[paramIndex]));
				break;
			}
		} catch (Exception ignored) {
		}
		returnTypes.put(key, new MissHolder<>(ret));
		return ret;
	}

	/**
	 * @return true if this type can ne instantiated (that is, it's a class and has a default constructor with not
	 * parameters)
	 */
	public boolean isInstantiatable() {
		if (isInstantiatableCache == null) 
			isInstantiatableCache = isInstantiableInternal();
		return isInstantiatableCache;
	}

	/**
	 * Internal implementation to determine if this type is instantiable.
	 */
	private boolean isInstantiableInternal() {
		if (!hasConcrete()) return false;
		Class<?> concrete = getConcrete();
		if (concrete == null) return false;
		if (concrete.isAnnotation() || concrete.isArray() || concrete.isEnum() || concrete.isInterface() || concrete.isPrimitive() || concrete.isSynthetic()) return false;
		try {
			Constructor<?> constructor = concrete.getConstructor();
			return constructor != null;
		} catch (NoSuchMethodException ignored) {
		}
		try {
			Constructor<?> constructor = concrete.getDeclaredConstructor();
			return constructor != null;
		} catch (NoSuchMethodException ignored) {
		}
		return false;
	}

	/**
	 * Creates a new instance for this type.
	 *
	 * @param <T> the actual type
	 * @return an instance
	 * @throws IllegalStateException if an instance cannot be created for whatever reason
	 */
	public <T> T newInstance() {
		T ret = null;
		Class<T> clazz = (Class<T>) getConcrete();
		try {
			ret = clazz.newInstance();
		} catch (Exception e) {
			try {
				Constructor<T> con = clazz.getDeclaredConstructor();
				con.setAccessible(true);
				ret = con.newInstance();
			} catch (NoSuchMethodException ignored) {
			} catch (Exception e2) {
				e = e2;
			}
			if (ret == null)
				throw new IllegalStateException("Cannot instantiate a " + clazz, e);
		}
		return ret;
	}

	/**
	 * @return true if a value of this type can be set to null, it must be concrete and not primitive
	 */
	public boolean isNullable() {
		return !getConcrete().isPrimitive();
	}

	/**
	 * @return true if this type is an enum
	 */
	public boolean isEnum() {
		return Enum.class.isAssignableFrom(getConcrete());
	}

	/**
	 * Gets an enum value by name.
	 *
	 * @param val the name of the value to get
	 * @return the value
	 * @throws IllegalStateException in case the value cannot be found
	 */
	public Enum<?> getEnumValue(String val) {
		if (enums == null) enums=(Enum<?>[]) getConcrete().getEnumConstants();
		for (Enum<?> v : enums) {
			if (v.name().equals(val)) return v;
		}
		throw new IllegalStateException("Cannot find enum value : " + getConcrete().getName() + "." + val);
	}

	/**
	 * @return true if this type is a char sequence (String, StringBuilder, StringBuffer etc..)
	 */
	public boolean isCharSequence() {
		return CharSequence.class.isAssignableFrom(getConcrete());
	}

	/**
	 * @return true if this type is a double (primitive double or Double)
	 */
	public boolean isDouble() {
		return Double.class == getConcrete() || Double.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is a float (primitive float or Float)
	 */
	public boolean isFloat() {
		return Float.class == getConcrete() || Float.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is a {@link BigDecimal}
	 */
	public boolean isBigDecimal() {
		return BigDecimal.class.isAssignableFrom(getConcrete());
	}

	/**
	 * @return true if this type is a integer (primitive int or Integer)
	 */
	public boolean isInteger() {
		return Integer.class == getConcrete() || Integer.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is a short (primitive short or Short)
	 */
	public boolean isShort() {
		return Short.class == getConcrete() || Short.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is a long (primitive long or Long)
	 */
	public boolean isLong() {
		return Long.class == getConcrete() || Long.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is a number (any type of number)
	 */
	public boolean isNumber() {
		return Number.class.isAssignableFrom(getConcrete());
	}

	/**
	 * @return true if this type is a boolean (primitive boolean or Boolean)
	 */
	public boolean isBoolean() {
		return Boolean.class == getConcrete() || Boolean.TYPE == getConcrete();
	}

	/**
	 * @return true if this type is an array
	 */
	public boolean isArray() {
		Class<?> conc;
		try {
			conc = getConcrete();
		} catch (Exception e) {
			return false;
		}
		return conc.isArray();
	}

	/**
	 * @return the constituent type of the array, for example for "Person[]" it will return the TypeUtil handling
	 * "Person"
	 */
	public TypeUtil getArrayType() {
		return get(getConcrete().getComponentType());
	}

	/**
	 * @return true if this type is a {@link List} or {@link Set}
	 */
	public boolean isListOrSet() {
		return (List.class.isAssignableFrom(this.getConcrete()) || Set.class.isAssignableFrom(this.getConcrete()));
	}

	/**
	 * @return the constituent type of the {@link List} or {@link Set}, for example for "List&lt;String&gt;" it will
	 * return the TypeUtil handling "String"
	 */
	public TypeUtil getListOrSetType() {
		TypeUtil inner = null;
		if (List.class.isAssignableFrom(this.getConcrete())) {
			inner = this.findReturnTypeOf("get", Integer.TYPE);
		} else if (Set.class.isAssignableFrom(this.getConcrete())) {
			inner = this.findParameterOf("add", 0);
		}
		return inner;
	}

	/**
	 * @return get the constituent type of this array or {@link List} or {@link Set}
	 */
	public TypeUtil getArrayListOrSetType() {
		if (this.isArray()) return this.getArrayType();
		if (this.isListOrSet()) return this.getListOrSetType();
		return null;
	}

	public boolean isDescendantOf(TypeUtil other) {
		return other.getConcrete().isAssignableFrom(this.getConcrete());
	}

	public boolean isParentOf(TypeUtil other) {
		return this.getConcrete().isAssignableFrom(other.getConcrete());
	}

	/**
	 * Inner class used to define inline types.
	 * <p>
	 * For example:
	 * <code>
	 *     new TypeUtil.Specific&lt;List&lt;String&gt;&gt;() {}.type();
	 * </code>
	 * @param <T> the actual inline type
	 */
	public static abstract class Specific<T> {
		public T mockGet() {
			return null;
		}
		public TypeUtil type() {
			return get(this.getClass()).findReturnTypeOf("mockGet");
		}
	}
	
	@Override
	public String toString() {
		return "TypeUtil[" + this.type + "]";
	}

}
