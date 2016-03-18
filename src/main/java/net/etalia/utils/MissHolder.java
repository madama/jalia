package net.etalia.utils;

public class MissHolder<T> {

	private T val;
	
	public MissHolder(T val) {
		this.val = val;
	}

	public T getVal() {
		return val;
	}

	@Override
	public int hashCode() {
		if (val == null) return 0;
		return val.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MissHolder)) return false;
		MissHolder oth = (MissHolder) obj;
		if (val == null) {
			return (oth.val == null);
		} else if (oth.val == null) return false;
		return val.equals(oth.val);
	}
}
