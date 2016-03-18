package net.etalia.jalia;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OutField {
	private OutField parent = null;
	private String name;
	private Map<String, OutField> subs;
	private boolean all;
	private boolean explicit;
	
	public OutField(OutField parent, String name) {
		this.parent = parent;
		this.name = name;
	}
	
	public OutField(OutField parent) {
		this(parent, "");
	}
	
	public OutField(OutField parent, boolean all) {
		this(parent, "");
		this.all = all;
	}
	
	public OutField(boolean all) {
		this(null,"*");
		this.all = all;
	}
	
	public void setAll(boolean all) {
		this.all = all;
	}
	
	public OutField getSub(String name) {
		if (all) return new OutField(this, true);
		if (subs == null) return null;
		int di = name.indexOf('.');
		if (di == -1) {
			return subs.get(name);
		} else {
			String prename = name.substring(0, di);
			OutField sub = subs.get(prename);
			if (sub == null) return null;
			return sub.getSub(name.substring(di+1));
		}
	}
	public OutField getCreateSub(String name) {
		if (all) throw new IllegalStateException("Can't add sub fields to *");
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
			if (mname.equals("*")) {
				this.all = true;
				this.explicit = true;
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

	public String getName() {
		return name;
	}
	public OutField getParent() {
		if (parent == null && all) return this;
		return parent;
	}
	public Map<String, OutField> getSubs() {
		return Collections.unmodifiableMap(subs);
	}
	
	public String getFullPath() {
		if (this.parent == null) return null;
		String pfp = this.parent.getFullPath();
		return (pfp != null ? (pfp + ".") : "") + this.name;
	}

	public boolean hasSubs() {
		return (this.all && this.explicit) || (subs != null && subs.size() > 0);
	}
	
	public Set<String> toStringList() {
		Set<String> ret = new HashSet<>();
		toStringList(ret);
		return ret;
	}
	
	public void toStringList(Set<String> set) {
		String mfp = getFullPath();
		if (mfp != null) set.add(mfp);
		if (subs != null) {
			for (OutField of : subs.values()) {
				of.toStringList(set);
			}
		}
	}

	public OutField getCreateSubs(String... split) {
		for (String sub : split) {
			getCreateSub(sub);
		}
		return this;
	}

	public static OutField getRoot(String... fields) {
		return new OutField(null).getCreateSubs(fields);
	}

	public Set<String> getSubsNames() {
		if (this.subs == null) return Collections.emptySet();
		return Collections.unmodifiableSet(this.subs.keySet());
	}
	
	public OutField reparent(String name) {
		OutField ret = new OutField(null);
		ret.subs = new HashMap<>();
		ret.subs.put(name, this);
		this.parent = ret;
		return ret;
	}
	
	public void reparentSubs(String name) {
		OutField nc = new OutField(this, name);
		nc.subs = new HashMap<>(this.subs);
		for (OutField sub : nc.subs.values()) {
			sub.parent = nc;
		}
		this.subs.clear();
		this.subs.put(name, nc);
	}
	
	public boolean isAll() {
		return all;
	}
}