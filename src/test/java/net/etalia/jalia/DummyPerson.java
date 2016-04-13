package net.etalia.jalia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.annotations.JsonRequireIdForReuse;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonOnDemandOnly;
import net.etalia.jalia.annotations.JsonSetter;


public class DummyPerson extends DummyEntity {

	private String name;
	private String surname;
	
	private List<DummyAddress> addresses = new ArrayList<>(); 
	private Set<String> tags = new HashSet<>();
	
	private Map<String,Object> extraData = null;
	private List<String> secrets = null;
	
	private List<DummyPerson> friends = new ArrayList<>();
	private DummyPerson bestFriend = null;
	private Integer age = null;
	private Float height = null;
	private Boolean active = null;
	
	private Date birthDay;
	
	public DummyPerson() {}
	
	public DummyPerson(String id, String name, String surname, DummyAddress... addresses) {
		super.setIdentifier(id);
		this.name = name;
		this.surname = surname;
		if (addresses != null) this.addresses.addAll(Arrays.asList(addresses));
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	
	public List<DummyAddress> getAddresses() {
		return addresses;
	}
	
	public Set<String> getTags() {
		return tags;
	}
	
	public DummyPerson initTags(String... tags) {
		this.tags.addAll(Arrays.asList(tags));
		return this;
	}

	public Map<String, Object> getExtraData() {
		if (extraData == null) return Collections.emptyMap();
		return Collections.unmodifiableMap(extraData);
	}
	
	@JsonSetter
	private void setExtraData(Map<String, Object> extraData) {
		this.extraData = extraData;
	}
	
	public List<String> getSecrets() {
		if (secrets == null) return Collections.emptyList();
		return Collections.unmodifiableList(secrets);
	}
	
	@JsonSetter
	private void setSecrets(List<String> secrets) {
		this.secrets = secrets;
	}
	
	@JsonRequireIdForReuse
	public List<DummyPerson> getFriends() {
		return friends;
	}
	
	@Override
	public String toString() {
		return "DummyPerson [name=" + name + ", surname=" + surname
				+ ", addresses=" + addresses + ", id="
				+ getIdentifier() + "]";
	}
	
	@JsonOnDemandOnly
	public List<DummyPerson> getSimilars() {
		return Arrays.asList(new DummyPerson("p1-1", "Test", "Similar"));
	}

	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	
	public Float getHeight() {
		return height;
	}
	public void setHeight(Float height) {
		this.height = height;
	}
	
	public Boolean getActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public DummyPerson getBestFriend() {
		return bestFriend;
	}
	public void setBestFriend(DummyPerson bestFriend) {
		this.bestFriend = bestFriend;
	}
	
	public Date getBirthDay() {
		return birthDay;
	}
	public void setBirthDay(Date birthDay) {
		this.birthDay = birthDay;
	}
	
}
