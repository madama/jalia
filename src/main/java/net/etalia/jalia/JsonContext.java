package net.etalia.jalia;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonWriter;

/**
 * Holds the context of current serialization or deserialization.
 *
 * Instances of this class are passed around a number of methods to give access to current state.
 *
 * This class models a number of properties as stacks. These stacks hold data for the current de-serialization step.
 * For example, consider the following entitites:
 *
 * <code>
 * class Person {
 *     String firstName;
 *     String lastName;
 *     List&lt;Address&gt; addresses;
 * }
 *
 * class Address {
 *     String city;
 *     String streetName;
 * }
 *
 * Person p = new Person();
 * // Setters
 * Address a1 = new Address();
 * // Setters
 * p.getAddresses().add(a1);
 *
 * objectMapper.writeValueAsString(p, OutField.getRoot("firstName","addresses.city"));
 * </code>
 *
 * When serializing these, it will start from the root object "p", initialized with default options from the
 * ObjectMapper, and with all the OutField given. When it will start serializing addresses, the serializer will invoke
 * the ObjectMapper again, and it will move the context into the property "addresses" and into the linked object "List".
 *
 * This will add elements to the stacks, so that while serializing the list little to no knowledge is needed about the
 * parent object serialization.
 *
 * Some information (like, for example, option to include nulls) need to be inherited, cause if they are set on the
 * ObjectMapper they must be observed on all the json tree when serializing any linked object.
 *
 * However, they can also be overridden at class or property level using annotations, so they could change for the
 * purpose of a specific branch of the json tree. As such, they are modeled as stacks.
 */
public class JsonContext extends HashMap<String, Object>{

	/**
	 * The ObjectMapper that started the activity.
	 */
	private final ObjectMapper mapper;

	/**
	 * The json output writer, set in case of serialization.
	 */
	private JsonWriter output;

	/**
	 * The json input reader, set in case of deserialization
	 */
	private JsonReader input;

	/**
	 * The root, complete, fields specification for serialization
	 */
	private OutField rootFields;

	/**
	 * Current, relative to current state, fields specifiecation for serialization
	 */
	private OutField currentFields;

	/**
	 * Counter of current deserialization "depth", incremented each time deserialization moves into a linked object.
	 */
	private int deserCount;

	/**
	 * Local, not inherited, stack of options. This stack is first checked by generic stack methods like
	 * {@link #getFromStack(String)}, and values there have precedence if they are set.
	 */
	private final Stack<Map<String,Object>> localStack = new Stack<Map<String,Object>>();

	/**
	 * Inherited stack of options. This stack is checked, and traversed upwards, by generic stack methods like
	 * {@link #getFromStack(String)} in case no specific value is set on the {@link #localStack}.
	 */
	private final Stack<Map<String,Object>> inheritStack = new Stack<Map<String,Object>>();

	/**
	 * Stack of property names used to produce meaningful error messages by {@link StateLog}.
	 */
	private final Stack<String> namesStack = new Stack<String>();

	/**
	 * State log for this context, will have access to private variables to produce meaningful error messages.
	 */
	private final StateLog stateLog = new StateLog();

	/**
	 * Creates a JsonContext.
	 *
	 * @param mapper the ObjectMapper for this context
	 */
	public JsonContext(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Adds an name-value pair in the given stack.
	 *
	 * @param stack a stack, usually {@link #localStack} or {@link #inheritStack}
	 * @param name the name to use
	 * @param obj the value to set
	 */
	protected void putInStack(Stack<Map<String,Object>> stack, String name, Object obj) {
		Map<String, Object> map = null;
		if (stack.size() > 0) {
			map = stack.peek();
		}
		if (map == null) {
			map = new HashMap<String, Object>();
			if (stack.size() > 0) stack.pop();
			stack.push(map);
		}
		map.put(name, obj);		
	}

	/**
	 * Puts a name-value in the local stack.
	 *
	 * @param name the name to use
	 * @param obj the value to set
	 */
	public void putLocalStack(String name, Object obj) {
		putInStack(localStack, name, obj);
	}

	/**
	 * Puts multiple name-values in the local stack.
	 *
	 * @param options map of name-values to set
	 */
	public void putLocalStack(Map<String, Object> options) {
		if (localStack.isEmpty()) localStack.push(null);
		if (options == null || options.isEmpty()) return;
		for (Map.Entry<String, Object> entry : options.entrySet()) {
			putLocalStack(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Puts a name-value in the inherited stack.
	 *
	 * @param name the name to use
	 * @param obj the value to set
	 */
	public void putInheritStack(String name, Object obj) {
		putInStack(inheritStack, name, obj);
	}

	/**
	 * Initializes the inherited stack using default values.
	 *
	 * @param options the default name-values to use
	 */
	public void initInheritStack(Map<String, Object> options) {
		inheritStack.push(options);
		inheritStack.push(null);
	}

	/**
	 * Gets a value from the stacks.
	 * <p>
	 * It will first look into the current level of the local stack. If not value is found there, then it
	 * will start searching, navigating upwards, the inherited stack.
	 *
	 * @param name the name to look up
	 * @return the value if found, null otherwise
	 */
	public Object getFromStack(String name) {
		if (localStack.size() == 0) return null;
		Map<String, Object> peek = localStack.peek();
		if (peek != null && peek.containsKey(name)) return peek.get(name);
		for (int i = inheritStack.size() - 1; i >= 0; i--) {
			peek = inheritStack.get(i);
			if (peek != null && peek.containsKey(name)) return peek.get(name);
		}
		return null;
	}

	/**
	 * Gets a boolean from the stack, convenience method to avoid casting and checking for nulls, follows the semantics
	 * of {@link #getFromStack(String)}.
	 *
	 * @param name the name to look up
	 * @return true only if value has been found and corresponds to a boolean "true", false in any other case
	 */
	public boolean getFromStackBoolean(String name) {
		Object obj = getFromStack(name);
		if (obj == null) return false;
		return (Boolean)obj;
	}

	/**
	 * Checks whether the given name-value is in the given stack, traversing the stack upwards.
	 *
	 * @param stack the stack to search
	 * @param name the name to search
	 * @param obj the value that must be matched
	 * @return true if the stack has, at any level, the given name-value pair
	 */
	protected boolean hasInStack(Stack<Map<String,Object>> stack, String name, Object obj) {
		for (int i = stack.size() - 1; i >= 0; i--) {
			Map<String, Object> peek = stack.get(i);
			if (peek != null && peek.get(name) == obj) return true;
		}
		return false;
	}

	/**
	 * Checks whether the given name-value is in the local stack, traversing the stack upwards.
	 *
	 * @param name the name to search
	 * @param obj the value that must be matched
	 * @return true if the stack has, at any level, the given name-value pair
	 */
	public boolean hasInLocalStack(String name, Object obj) {
		return hasInStack(localStack, name, obj);
	}

	/**
	 * Checks whether the given name-value is in the inherited stack, traversing the stack upwards.
	 *
	 * @param name the name to search
	 * @param obj the value that must be matched
	 * @return true if the stack has, at any level, the given name-value pair
	 */
	public boolean hasInInheritStack(String name, Object obj) {
		return hasInStack(inheritStack, name, obj);
	}

	/**
	 * Sets the output json writer.
	 *
	 * @param output the output writer in use
	 */
	// TODO move to constructor?
	public void setOutput(JsonWriter output) {
		this.output = output;
	}

	/**
	 * Gets the output writer being used in serialization.
	 *
	 * @return the output writer in use
	 */
	public JsonWriter getOutput() {
		return output;
	}

	/**
	 * Sets the input json reader.
	 *
	 * @param input the input reader in use
	 */
	// TODO move to constructor?
	public void setInput(JsonReader input) {
		this.input = input;
	}

	/**
	 * Gets the input reader being used in deserialization.
	 *
	 * @return the input reader in use
	 */
	public JsonReader getInput() {
		return input;
	}

	/**
	 * Sets the root {@link OutField} settings to use during serialization.
	 *
	 * @param rootField the root OutField to use
	 */
	// TODO move to constructor?
	public void setRootFields(OutField rootField) {
		rootFields = rootField;
		currentFields = rootField;
	}

	/**
	 * Gets the root {@link OutField} settings in use during serialization.
	 *
	 * @return the root OutField in use
	 */
	public OutField getRootFields() {
		return rootFields;
	}

	/**
	 * Gets the current {@link OutField} settings, relative to current step of serialization.
	 *
	 * @return the OutField for the current serialziation step
	 */
	public OutField getCurrentFields() {
		return currentFields;
	}

	/**
	 * @return the ObjectMapped in this context
	 */
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Used to signal that a serializer is entering serialization of a property.
	 *
	 * @param fieldName the name of the property about to be serialized
	 * @param defaults default fields to serialize
	 * @return true if the serializer should proceed serializing the field
	 */
	public boolean entering(String fieldName, String... defaults) {
		return entering(fieldName, defaults == null ? null : Arrays.asList(defaults));
	}

	/**
	 * Used to signal that a serializer is entering serialization of a property.
	 *
	 * @param fieldName the name of the property about to be serialized
	 * @param defaults default fields to serialize
	 * @return true if the serializer should proceed serializing the field
	 */
	public boolean entering(String fieldName, Collection<String> defaults) {
		//if (acsub == null)
			// If there are no defined children for this field
			if (!currentFields.hasSubs()) {
				// If we have some defaults to use
				if (defaults != null && defaults.size() > 0) {
					// apply those defaults
					currentFields.setAll(false);
					for (String def : defaults) {
						currentFields.getCreateSub(def);
					}
					// Re-execute so that the "entering" can be re-evaluated based on new default children
					return entering(fieldName);
				}
			}
			OutField acsub = currentFields.getSub(fieldName);
			if (acsub == null) {
				return false;
			}
		//}
		// Move the stacks
		currentFields = acsub;
		localStack.push(null);
		inheritStack.push(null);
		namesStack.push(fieldName);
		return true;
	}

	/**
	 * @return true if, given the current OutField settings, all found properties in the current object must be
	 * serialized
	 */
	public boolean isSerializingAll() {
		return currentFields.isAll();
	}

	/**
	 * @return a list of names of all the properties that need to be serialied
	 */
	public Set<String> getCurrentSubs() {
		return currentFields.getSubsNames();
	}

	/**
	 * Used to signal that a serializer has finished serializing current property.
	 */
	public void exited() {
		currentFields = currentFields.getParent();
		localStack.pop();
		inheritStack.pop();
		namesStack.pop();
	}

	/**
	 * Used to signal that a deserialized is about to deserialize a property.
	 *
	 * @param name name of the property about to be deserialized
	 */
	public void deserializationEntering(String name) {
		deserCount++;
		localStack.push(null);
		inheritStack.push(null);
		namesStack.push(name);
	}

	/**
	 * Used to signal that a deserializer has finished deserializing current property.
	 */
	public void deserializationExited() {
		deserCount--;
		namesStack.pop();
		localStack.pop();
		inheritStack.pop();
	}


	/**
	 * @return true if this context is at the root of the de-serialization process
	 */
	public boolean isRoot() {
		if (currentFields != null) {
			return currentFields.getParent() == null || currentFields.getParent() == currentFields;
		} 
		return deserCount == 0;
	}

	/**
	 * Inner class used to access private fileds to produce meaningful error messages.
	 */
	public class StateLog {
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append(namesStack.toString());
			if (input != null)
				ret.append(" @").append(input.getLineNumber()).append(":").append(input.getColumnNumber());
			return ret.toString();
		}
	}

	/**
	 * @return the StageLog of this context
	 */
	public StateLog getStateLog() {
		return stateLog;
	}

	public JsonContext subForInput(JsonReader newInput) {
		JsonContext ret = new JsonContext(mapper);
		ret.currentFields = currentFields;
		ret.deserCount = deserCount;
		ret.inheritStack.addAll(inheritStack);
		ret.input = newInput;
		ret.localStack.addAll(localStack);
		ret.namesStack.addAll(namesStack);
		ret.output = output;
		ret.rootFields = rootFields;
		return ret;
	}

}
