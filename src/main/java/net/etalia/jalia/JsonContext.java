package net.etalia.jalia;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonWriter;

public class JsonContext extends HashMap<String, Object>{

	private ObjectMapper mapper;
	private JsonWriter output;
	private JsonReader input;
	private OutField rootFields;
	
	private OutField currentFields;
	private int deserCount;
	
	private Stack<Map<String,Object>> localStack = new Stack<Map<String,Object>>();
	private Stack<Map<String,Object>> inheritStack = new Stack<Map<String,Object>>();
	private Stack<String> namesStack = new Stack<String>();
	
	private StateLog stateLog = new StateLog();
	
	public JsonContext(ObjectMapper mapper) {
		this.mapper = mapper;
	}

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
	
	public void putLocalStack(String name, Object obj) {
		putInStack(localStack, name, obj);
	}
	
	public void putLocalStack(Map<String, Object> options) {
		if (localStack.isEmpty()) localStack.push(null);
		if (options == null || options.isEmpty()) return;
		for (Map.Entry<String, Object> entry : options.entrySet()) {
			putLocalStack(entry.getKey(), entry.getValue());
		}
	}	

	public void putInheritStack(String name, Object obj) {
		putInStack(inheritStack, name, obj);
	}

	public void initInheritStack(Map<String, Object> options) {
		inheritStack.push(options);
		inheritStack.push(null);
	}	
	
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
	
	public boolean getFromStackBoolean(String name) {
		Object obj = getFromStack(name);
		if (obj == null) return false;
		return (Boolean)obj;
	}
	
	protected boolean hasInStack(Stack<Map<String,Object>> stack, String name, Object obj) {
		for (int i = stack.size() - 1; i >= 0; i--) {
			Map<String, Object> peek = stack.get(i);
			if (peek != null && peek.get(name) == obj) return true;
		}
		return false;
	}
	
	public boolean hasInLocalStack(String name, Object obj) {
		return hasInStack(localStack, name, obj);
	}
	
	public boolean hasInInheritStack(String name, Object obj) {
		return hasInStack(inheritStack, name, obj);
	}
	
	public void setOutput(JsonWriter output) {
		this.output = output;
	}
	public JsonWriter getOutput() {
		return output;
	}
	
	public void setInput(JsonReader input) {
		this.input = input;
	}
	public JsonReader getInput() {
		return input;
	}
	
	public void setRootFields(OutField rootField) {
		this.rootFields = rootField;
		this.currentFields = rootField;
	}
	public OutField getRootFields() {
		return rootFields;
	}
	public OutField getCurrentFields() {
		return this.currentFields;
	}
	
	public ObjectMapper getMapper() {
		return mapper;
	}

	public boolean entering(String fieldName, String... defaults) {
		return this.entering(fieldName, defaults == null ? null : Arrays.asList(defaults));
	}

	public boolean entering(String fieldName, Collection<String> defaults) {
		//if (acsub == null) {
			if (!currentFields.hasSubs()) {
				if (defaults != null && defaults.size() > 0) {
					currentFields.setAll(false);
					for (String def : defaults) {
						currentFields.getCreateSub(def);
					}
					return entering(fieldName);
				}
			}
			OutField acsub = currentFields.getSub(fieldName);
			if (acsub == null)
				return false;
		//}
		currentFields = acsub;
		localStack.push(null);
		inheritStack.push(null);
		namesStack.push(fieldName);
		return true;
	}
	
	public boolean isSerializingAll() {
		return currentFields.isAll();
	}

	
	
	public Set<String> getCurrentSubs() {
		return currentFields.getSubsNames();
	}
	
	public void exited() {
		currentFields = currentFields.getParent();
		localStack.pop();
		inheritStack.pop();
		namesStack.pop();
	}

	public void deserializationEntering(String name) {
		this.deserCount++;
		localStack.push(null);
		inheritStack.push(null);
		namesStack.push(name);
	}
	
	public void deserializationExited() {
		this.deserCount--;
		namesStack.pop();
		localStack.pop();
		inheritStack.pop();
	}
	

	public boolean isRoot() {
		if (currentFields != null) {
			return currentFields.getParent() == null || currentFields.getParent() == currentFields;
		} 
		return deserCount == 0;
	}

	public class StateLog {
		public String toString() {
			StringBuilder ret = new StringBuilder();
			if (namesStack != null)
				ret.append(namesStack.toString());
			if (input != null)
				ret.append(" @" + input.getLineNumber() + ":" + input.getColumnNumber());
			return ret.toString();
		}
	}
	
	public StateLog getStateLog() {
		return stateLog;
	}

}
