package net.etalia.jalia;

import java.util.ArrayList;
import java.util.List;

import net.etalia.jalia.annotations.JsonDefaultFields;
import net.etalia.jalia.annotations.JsonGetter;
import net.etalia.jalia.annotations.JsonIgnore;
import net.etalia.jalia.annotations.JsonIgnoreProperties;
import net.etalia.jalia.annotations.JsonInclude;
import net.etalia.jalia.annotations.JsonSetter;
import net.etalia.jalia.annotations.JsonInclude.Include;

@JsonIgnoreProperties({"hidden1","hidden2"})
@JsonDefaultFields("both")
public class DummyAnnotations {

	private String both;
	private String secretByGetter;
	private String secretBySetter;
	private String getOnly;
	private String getOnlyByGetter;
	private String setOnly;
	private String setOnlyBySetter;
	private String unusualGetter;
	
	private String hidden1;
	private String hidden2;
	
	private String alternative;
	
	private boolean natBoolean;
	private Boolean objBoolean;
	
	private List<String> inclAlways = new ArrayList<String>();
	private List<String> inclNotNull = new ArrayList<String>();
	private List<String> inclNotEmpty = new ArrayList<String>();
	
	public String getBoth() {
		return both;
	}
	public void setBoth(String both) {
		this.both = both;
	}

	@JsonIgnore
	public String getSecretByGetter() {
		return secretByGetter;
	}
	public void setSecretByGetter(String secretByGetter) {
		this.secretByGetter = secretByGetter;
	}
	
	public String getSecretBySetter() {
		return secretBySetter;
	}
	@JsonIgnore
	public void setSecretBySetter(String secretBySetter) {
		this.secretBySetter = secretBySetter;
	}

	@JsonIgnore(false)
	public String getGetOnly() {
		return getOnly;
	}
	@JsonIgnore
	public void setGetOnly(String getOnly) {
		this.getOnly = getOnly;
	}
	
	@JsonGetter
	public String getGetOnlyByGetter() {
		return getOnlyByGetter;
	}
	@JsonIgnore
	public void setGetOnlyByGetter(String getOnlyByGetter) {
		this.getOnlyByGetter = getOnlyByGetter;
	}
	
	@JsonIgnore
	public String getSetOnly() {
		return setOnly;
	}
	@JsonIgnore(false)
	public void setSetOnly(String setOnly) {
		this.setOnly = setOnly;
	}
	
	@JsonSetter
	public void setSetOnlyBySetter(String setOnlyBySetter) {
		this.setOnlyBySetter = setOnlyBySetter;
	}
	@JsonIgnore
	public String getSetOnlyBySetter() {
		return setOnlyBySetter;
	}
	
	@JsonGetter("unusual")
	public String unusualGetter() {
		return unusualGetter;
	}
	@JsonSetter("unusual")
	public void unusualSetter(String unusualGetter) {
		this.unusualGetter = unusualGetter;
	}
	
	public String getHidden1() {
		return hidden1;
	}
	public void setHidden1(String hidden1) {
		this.hidden1 = hidden1;
	}
	public String getHidden2() {
		return hidden2;
	}
	public void setHidden2(String hidden2) {
		this.hidden2 = hidden2;
	}
	
	public String getAlternative() {
		return alternative;
	}
	public void setAlternative(String alternative) {
		this.alternative = alternative;
	}
	@JsonSetter
	public void setAlternative(int alternative) {
		this.alternative = Integer.toString(alternative);
	}

	public boolean isNatBoolean() {
		return natBoolean;
	}
	public void setNatBoolean(boolean natBoolean) {
		this.natBoolean = natBoolean;
	}
	
	public Boolean isObjBoolean() {
		return objBoolean;
	}
	public void setObjBoolean(Boolean objBoolean) {
		this.objBoolean = objBoolean;
	}
	
	@JsonInclude(Include.ALWAYS)
	public List<String> getInclAlways() {
		return inclAlways;
	}
	public void setInclAlways(List<String> evenIfEmpty) {
		this.inclAlways = evenIfEmpty;
	}

	@JsonInclude(Include.NOT_EMPTY)
	public List<String> getInclNotEmpty() {
		return inclNotEmpty;
	}
	public void setInclNotEmpty(List<String> inclNotEmpty) {
		this.inclNotEmpty = inclNotEmpty;
	}
	
	@JsonInclude(Include.NOT_NULL)
	public List<String> getInclNotNull() {
		return inclNotNull;
	}
	public void setInclNotNull(List<String> inclNotNull) {
		this.inclNotNull = inclNotNull;
	}
}
