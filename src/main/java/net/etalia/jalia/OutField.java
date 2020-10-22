package net.etalia.jalia;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds which fields are required to be serialized.
 * <p>
 * Fields can be defined in a string format, like for example:
 * <code>
 *     firstName,lastName,father,mother,addresses,
 * </code>
 * Linked entities will use default fields, unless further specified, for example:
 * <code>
 *     father.firstName,mother.firstName
 * </code>
 * in which case only required fileds of linked entities will be serialized.
 * <p>
 * Moreover, a "*" can se used meaning all fields:
 * <code>
 *     addresses.*
 * </code>
 * <p>
 * Required fields are organized in a tree that will be lazily constructed parsing the "." in the fields definition.
 *
 */
public class OutField {

	private static final Map<String, OutField> groups = new HashMap<>();

	/**
	 * The parent (in the tree of OutField definitions) fo this definition, or null for the root definition.
	 */
	private OutField parent;

	/**
	 * Name of this field, or empty string for the root definition.
	 */
	private final String name;

	/**
	 * Map of children of this definition.
	 */
	private Map<String, OutField> subs;

	/**
	 * Set to true if all the children of the field representing this definition must be serialized.
	 */
	private boolean all;

	/**
	 * Set to true if a sepcific definition was given for this field, false if instead default fields are being
	 * serialized.
	 */
	private boolean explicit;

	/**
	 * @param parent the parent definition
	 * @param name the field name
	 */
	public OutField(OutField parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	/**
	 * @param parent the parent definition
	 */
	public OutField(OutField parent) {
		this(parent, "");
	}

	/**
	 * @param parent the parent definition
	 * @param all true if all the fields must be serialized, false if only defined or default fields must be serialized.
	 */
	public OutField(OutField parent, boolean all) {
		this(parent, "");
		this.all = all;
	}

	/**
	 * Creates a root definition where all fields must be serialized.
	 *
	 * @param all true if all the fields must be serialized, false if only defined or default fields must be serialized.
	 */
	public OutField(boolean all) {
		this(null,"*");
		this.all = all;
	}

	/**
	 * @param all true if all the fields must be serialized, false if only defined or default fields must be serialized.
	 */
	public void setAll(boolean all) {
		this.all = all;
	}

	/**
	 * Search an existing children of this definition for the given field name.
	 *
	 * @param name the name of the field or part of the definition, like "father.firstName"
	 * @return the definition for the field, or null if not found
	 */
	public OutField getSub(String name) {
		// In case all fields need to be serialized, initialize a throw-away definition for "all"
		if (all) return new OutField(this, true);

		if (subs == null) return null;
		int di = name.indexOf('.');
		if (di == -1) {
			// No sub-definition, simple case of field name, search it in the children map
			return subs.get(name);
		} else {
			// Sub-definition case, find the immediate children and delegate to it
			String prename = name.substring(0, di);
			OutField sub = subs.get(prename);
			if (sub == null) return null;
			return sub.getSub(name.substring(di+1));
		}
	}

	/**
	 * Search an existing children of this definition for the given field name.
	 *
	 * @param name the name of the field or part of the definition, like "father.firstName"
	 * @return the definition for the field, or null if not found
	 */
	public OutField getCreateSub(String name) {
		if (all) {
			throw new IllegalStateException("Can't add sub fields to * on " + getFullPath());
		}
		if (subs == null) {
			subs = new HashMap<>();
		}
		int di = name.indexOf('.');
		String mname = name;
		if (di > -1) {
			mname = name.substring(0, di);
		}
		OutField ret = subs.get(mname);
		if (ret == null) {
			// This is the case in which it's discovered that one of the "subfields" is actually "*". The need for this
			// apparently strange dynamic where searchign for a subfield ends up changing the state of "this", happens
			// to rectify the syntax "father.*" where ".*" is not a subfield but the declaration that "father" should
			// have "all=true".
			if (mname.equals("*")) {
				all = true;
				explicit = true;
				ret = this;
			} else {
				ret = new OutField(this, mname);
				subs.put(mname, ret);
			}
		}
		if (di == -1) {
			return ret;
		} else {
			return ret.getCreateSub(name.substring(di+1));
		}
	}

	/**
	 * @return the name of the field represented by this instance
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the parent of this instance on the OutField tree
	 */
	public OutField getParent() {
		if (parent == null && all) return this;
		return parent;
	}

	/**
	 * @return current maps of subfields, used only in tests
	 */
	protected Map<String, OutField> getSubs() {
		return Collections.unmodifiableMap(subs);
	}

	/**
	 * @return the "fully qualified" path, similar to the one used in the original definition, of this OutField.
	 */
	protected String getFullPath() {
		if (parent == null) {
			return null;
		}
		String pfp = parent.getFullPath();
		return (pfp != null ? (pfp + ".") : "") + name;
	}

	/**
	 * @return true if this field has some subfields required to be serialized, false if it doesn't have any and
	 * defaults should be used.
	 */
	public boolean hasSubs() {
		return (all && explicit) || (subs != null && subs.size() > 0);
	}

	/**
	 * @return a list of "fully qualified" strings, similar to original definitions, of this OutField and its children.
	 */
	protected Set<String> toStringList() {
		Set<String> ret = new HashSet<>();
		toStringList(ret);
		return ret;
	}

	/**
	 * Fill the set with "fully qualified" strings, similar to original definitions, of this OutField and its children.
	 * @param set the set to fill
	 */
	protected void toStringList(Set<String> set) {
		String mfp = getFullPath();
		if (mfp != null) set.add(mfp);
		if (subs != null) {
			for (OutField of : subs.values()) {
				of.toStringList(set);
			}
		}
	}

	/**
	 * Get or creates multiple children of this OutField based on the given definitions.
	 *
	 * @param definitions the definitions to parse
	 * @return this same instance
	 */
	public OutField getCreateSubs(String... definitions) {
		for (String sub : definitions) {
			getCreateSub(sub);
		}
		return this;
	}

	/**
	 * Creates a root OutField complete with all the children and grandchildren parsig the given definitions.
	 *
	 * @param fields the definitions to parse
	 * @return a new root OutField
	 */
	public static OutField getRoot(String... fields) {
		return new OutField(null).getCreateSubs(fields);
	}

	/**
	 * Access to groups configuration.
	 * <p>
	 * This map can be modified to add new groups programmatically.
	 *
	 * @return the map with currently configured groups
	 */
	public static Map<String, OutField> getGroups() {
		return groups;
	}

	/**
	 * Parses a JSON containing group configurations and add it to the current groups.
	 * <p>
	 *     JSON format is rather flexible, accepting simple arrays or comma separated lsits:
	 *     <code>
	 *         {
	 *             "groupName": ["field1", "field2", "link.field1", "link.field2", "otherLink.*"],
	 *             "otherGroup": "field1,field2"
	 *         }
	 *     </code>
	 *     As well as nested objects:
	 *     <code>
	 *         {
	 *             "groupName": {
	 *                 "field1": true,
	 *                 "field2": true,
	 *                 "link": {
	 *                     "field1": true,
	 *                     "field2": true
	 *                 },
	 *                 "otherLink": {
	 *                     "*" : true
	 *                 }
	 *             }
	 *         }
	 *     </code>
	 *     As well as mixed:
	 *     <code>
	 *         {
	 *             "groupName": [
	 *                 ["field1","field2"],
	 *                 {"link": ["field1","field2"]},
	 *                 {"otherLink": "*"}
	 *             ]
	 *         }
	 *     </code>
	 * </p>
	 * @param reader
	 */
	public static void parseGroupsJson(Reader reader) {
		ObjectMapper om = new ObjectMapper();
		om.setOption(DefaultOptions.LENIENT_READER, true);
		Map<String,Object> payload = om.readValue(reader, new TypeUtil.Specific<Map<String, Object>>() {}.type());
		for (Map.Entry<String, Object> entry : payload.entrySet()) {
			List<String> definitions = new ArrayList<>();
			recurseParseGroupJson(entry.getValue(), "", definitions);
			String groupName = entry.getKey();
			OutField fields = groups.get(groupName);
			if (fields == null) {
				fields = new OutField(null);
			}
			fields.getCreateSubs(definitions.toArray(new String[definitions.size()]));
			groups.put(groupName, fields);
		}
	}

	public static void cleanGroups() {
		groups.clear();
	}

	private static void recurseParseGroupJson(Object value, String prefix, List<String> definitions) {
		if (value instanceof Boolean) {
			definitions.add(prefix.substring(0, prefix.length() - 1));
		} else if (value instanceof String) {
			String[] segments = ((String) value).split(",");
			for (String segment : segments) {
				segment = segment.trim();
				definitions.add(prefix + segment);
			}
		} else if (value instanceof Iterable) {
			for (Object entry : (Iterable)value) {
				recurseParseGroupJson(entry, prefix, definitions);
			}
		} else if (value instanceof Map) {
			for (Map.Entry<String, Object> entry : ((Map<String,Object>) value).entrySet()) {
				String key = entry.getKey().trim();
				if (key.equals("__")) {
					key = "";
				} else {
					key = key + ".";
				}
				recurseParseGroupJson(entry.getValue(), prefix + key, definitions);
			}
		}
	}

	/**
	 * @return list of defined children
	 */
	public Set<String> getSubsNames() {
		if (subs == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(subs.keySet());
	}

	/**
	 * Creates a new parent, with the given name, for this instance.
	 *
	 * @param name the name of the new parent
	 * @return the newly created parent
	 */
	// TODO remove?
	public OutField reparent(String name) {
		OutField ret = new OutField(null);
		ret.subs = new HashMap<>();
		ret.subs.put(name, this);
		parent = ret;
		return ret;
	}

	/**
	 * Creates a new parent, with the given names, and transfers all the children of this instance to that new parent.
	 *
	 * @param name the name of the new parent
	 * @return the newly created parent
	 */
	// TODO remove?
	public OutField reparentSubs(String name) {
		OutField nc = new OutField(this, name);
		nc.subs = new HashMap<>(subs);
		for (OutField sub : nc.subs.values()) {
			sub.parent = nc;
		}
		subs.clear();
		subs.put(name, nc);
		return nc;
	}

	/**
	 * @return true if this OutField must include all of the found fields
	 */
	public boolean isAll() {
		return all;
	}
}