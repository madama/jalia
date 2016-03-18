package net.etalia.jalia;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonIgnore;

@JsonDefaultFields("identifier,type,address")
public class DummyAddress extends DummyEntity {

	public enum AddressType {
		HOME,
		OFFICE,
		EMAIL
	}
	
	public static AddressType getDefaultType() {
		return AddressType.HOME;
	}
	
	private AddressType type;
	private String address;
	private String notes;
	private Set<String> tags = new HashSet<String>();
	
	private DummyAddress() {}
	
	public DummyAddress(String id, AddressType type, String address) {
		this.setIdentifier(id);
		this.type = type;
		this.address = address;
	}
	
	public AddressType getType() {
		return type;
	}
	public void setType(AddressType type) {
		this.type = type;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public Set<String> getTags() {
		return tags;
	}
	
	public DummyAddress initTags(String... tags) {
		this.tags.addAll(Arrays.asList(tags));
		return this;
	}	
	

	@Override
	public String toString() {
		return "DummyAddress [type=" + type + ", address=" + address
				+ ", id=" + getIdentifier() + "]";
	}
	
	
}
