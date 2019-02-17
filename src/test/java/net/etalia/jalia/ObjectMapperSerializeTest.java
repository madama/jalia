package net.etalia.jalia;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.etalia.jalia.DummyAddress.AddressType;
import org.junit.Test;

public class ObjectMapperSerializeTest extends TestBase {
	
	public enum TestEnum {
		VAL1,
		VAL2
	}

	@Test
	public void simpleMap() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Map<String,Object> map = new HashMap<>();
		
		map.put("testString", "string");
		map.put("testInt", 1);
		map.put("testBoolean", true);
		map.put("testLong", 100l);
		map.put("testEnum", TestEnum.VAL1);
		
		Map<String,Object> submap = new HashMap<>();
		submap.put("subString", "string");
		
		map.put("testMap", submap);
		
		map.put("subEmptyList", new ArrayList<String>());
		
		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, null, map);
		
		String json = writer.toString();
		System.out.println(json);
		
		checkThat(json, containsString("\"testEnum\":"));
		checkThat(json, containsString("\"VAL1\""));
		
		checkThat(json, containsString("\"testString\":"));
		checkThat(json, containsString("\"string\""));
		
		checkThat(json, containsString("\"testInt\":"));
		checkThat(json, containsString("1"));
		
		checkThat(json, containsString("\"testBoolean\":"));
		checkThat(json, containsString("true"));
		
		checkThat(json, containsString("\"testLong\":"));
		checkThat(json, containsString("100"));
		
		checkThat(json, containsString("\"testMap\":"));
		checkThat(json, containsString("\"subString\":"));
		
		checkThat(json, containsString("\"subEmptyList\":"));
		checkThat(json, containsString("[]"));
	}
	
	@Test
	public void simpleList() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		List<Object> list = new ArrayList<>();
		
		list.add("string");
		list.add(1);
		
		List<Object> sublist = new ArrayList<>();
		sublist.add("substring");
		list.add(sublist);
		
		list.add(new String[] { "arr1", "arr2" });
		
		StringWriter sw = new StringWriter();
		mapper.writeValue(sw, null, list);
		
		String json = sw.toString();
		System.out.println(json);
		
		checkThat(json, containsString("\"string\""));
		checkThat(json, containsString("\"substring\""));
		checkThat(json, containsString("\"arr1\""));
		checkThat(json, containsString("\"arr2\""));
		checkThat(json, containsString("1"));
		
	}
	
	@Test
	public void mapWithOutFields() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Map<String,Object> map = new HashMap<>();
		
		map.put("testString", "string");
		map.put("testInt", 1);
		map.put("testBoolean", true);
		map.put("testLong", 100l);
		map.put("testEnum", TestEnum.VAL1);
		
		Map<String,Object> submap = new HashMap<>();
		submap.put("subString", "string");
		
		map.put("testMap", submap);
		
		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testBoolean");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			checkThat(json, containsString("\"testString\":"));
			checkThat(json, containsString("\"string\""));
			
			checkThat(json, containsString("\"testBoolean\":"));
			checkThat(json, containsString("true"));
			
			checkThat(json, not(containsString("\"testEnum\":")));
			checkThat(json, not(containsString("\"VAL1\"")));
			
			checkThat(json, not(containsString("\"testInt\":")));
			checkThat(json, not(containsString("1")));
			
			checkThat(json, not(containsString("\"testLong\":")));
			checkThat(json, not(containsString("100")));
			
			checkThat(json, not(containsString("\"testMap\":")));
			checkThat(json, not(containsString("\"subString\":")));
		}
		
		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testMap.subString");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			checkThat(json, containsString("\"testString\":"));
			checkThat(json, containsString("\"string\""));
			
			checkThat(json, not(containsString("\"testBoolean\":")));
			checkThat(json, not(containsString("true")));
			
			checkThat(json, not(containsString("\"testEnum\":")));
			checkThat(json, not(containsString("\"VAL1\"")));
			
			checkThat(json, not(containsString("\"testInt\":")));
			checkThat(json, not(containsString("1")));
			
			checkThat(json, not(containsString("\"testLong\":")));
			checkThat(json, not(containsString("100")));
			
			checkThat(json, containsString("\"testMap\":"));
			checkThat(json, containsString("\"subString\":"));
		}

		{
			OutField fields = new OutField(null, "");
			fields.getCreateSub("testString");
			fields.getCreateSub("testMap");
			
			StringWriter writer = new StringWriter();
			mapper.writeValue(writer, fields, map);
			
			String json = writer.toString();
			System.out.println(json);
			
			checkThat(json, containsString("\"testString\":"));
			checkThat(json, containsString("\"string\""));
			
			checkThat(json, not(containsString("\"testBoolean\":")));
			checkThat(json, not(containsString("true")));
			
			checkThat(json, not(containsString("\"testEnum\":")));
			checkThat(json, not(containsString("\"VAL1\"")));
			
			checkThat(json, not(containsString("\"testInt\":")));
			checkThat(json, not(containsString("1")));
			
			checkThat(json, not(containsString("\"testLong\":")));
			checkThat(json, not(containsString("100")));
			
			checkThat(json, containsString("\"testMap\":"));
			checkThat(json, containsString("\"subString\":"));
		}
		
	}
	
	private DummyPerson makePerson() {
		DummyPerson person = new DummyPerson();
		person.setName("Simone");
		person.setSurname("Gianni");
		person.setIdentifier("p1");
		
		DummyAddress address = new DummyAddress("a1", AddressType.EMAIL, "simoneg@apache.org");
		address.setNotes("Doremi");
		address.initTags("addressTag1","addressTag2");
		person.getAddresses().add(address);
		
		return person;
	}
	
	@Test
	public void simpleObject() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		DummyPerson person = makePerson();
		person.initTags("tag1","tag2");
		person.setBirthDay(new Date(1000));
		
		String json = mapper.writeValueAsString(person, null);
		System.out.println(json);
		
		checkThat(json, containsString("\"name\":"));
		checkThat(json, containsString("\"Simone\""));
		
		checkThat(json, containsString("\"surname\":"));
		checkThat(json, containsString("\"Gianni\""));
		
		checkThat(json, containsString("\"addresses\":"));
		
		checkThat(json, containsString("\"address\":"));
		checkThat(json, containsString("\"simoneg@apache.org\""));
		
		checkThat(json, containsString("\"type\":"));
		checkThat(json, containsString("\"EMAIL\""));
		
		checkThat(json, containsString("\"identifier\":"));
		checkThat(json, containsString("\"p1\""));
		checkThat(json, containsString("\"a1\""));

		checkThat(json, containsString("\"tags\":"));
		checkThat(json, containsString("\"tag1\""));
		checkThat(json, containsString("\"tag2\""));
		
		checkThat(json, containsString("\"birthDay\":1000"));
		
		checkThat(json, not(containsString("\"defaultType\"")));
		
	}
	
	@Test
	public void nullsOnObject() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		DummyPerson person = makePerson();
		person.setSurname(null);
		
		String json = om.writeValueAsString(person);
		checkThat(json, not(containsString("\"surname\":")));
		
		om.setOption(DefaultOptions.INCLUDE_NULLS, true);
		json = om.writeValueAsString(person);
		checkThat(json, containsString("\"surname\":"));
		checkThat(json, containsString("null"));
	}
	
	
	@Test
	public void empties() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		DummyPerson person = makePerson();
		person.getAddresses().clear();
		
		String json = om.writeValueAsString(person);
		checkThat(json, not(containsString("\"addresses\":")));
	}
	
	@Test
	public void emptyRoots() throws Exception {
		ObjectMapper om = new ObjectMapper();

		{
			String json = om.writeValueAsString(new ArrayList<String>());
			checkThat(json, equalTo("[]"));
		}
		{
			String json = om.writeValueAsString(new String[] {});
			checkThat(json, equalTo("[]"));
		}
		{
			String json = om.writeValueAsString(new HashMap<String,String>());
			checkThat(json, equalTo("{}"));
		}
	}
	
	@Test
	public void objectLoop() throws Exception {
		DummyPerson person1 = new DummyPerson();
		DummyPerson person2 = new DummyPerson();
		
		person1.setIdentifier("p1");
		person2.setIdentifier("p2");
		
		person1.getFriends().add(person2);
		person2.getFriends().add(person1);
		
		ObjectMapper om = new ObjectMapper();
		om.setOption(DefaultOptions.UNROLL_OBJECTS, true);
		DummyEntityProvider prov = new DummyEntityProvider();
		om.setEntityNameProvider(prov);
		om.setEntityFactory(prov);
		om.setClassDataFactory(prov);
		
		String json = null;
		try {
			json = om.writeValueAsString(person1);
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getClass().getName() + " : " + t.getMessage());
		}
		
		System.out.println(json);
		
		checkThat(json,containsString("p1"));
		checkThat(json,containsString("p2"));
		checkThat(json,containsString("[\"p1\"]"));
	}
	
	@Test
	public void objectLoopWithFields() throws Exception {
		DummyPerson person1 = new DummyPerson();
		DummyPerson person2 = new DummyPerson();
		
		person1.setIdentifier("p1");
		person2.setIdentifier("p2");
		
		person1.getFriends().add(person2);
		person2.getFriends().add(person1);
		
		ObjectMapper om = new ObjectMapper();
		om.setOption(DefaultOptions.UNROLL_OBJECTS, true);
		DummyEntityProvider prov = new DummyEntityProvider();
		om.setEntityNameProvider(prov);
		om.setEntityFactory(prov);
		om.setClassDataFactory(prov);
		
		String json = null;
		try {
			json = om.writeValueAsString(person1, OutField.getRoot("friends.id","friends.friends.id"));
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getClass().getName() + " : " + t.getMessage());
		}
		
		System.out.println(json);

		int fp1 = json.indexOf("p1");
		int fp2 = json.indexOf("p2");
		int sp1 = json.indexOf("p1", fp2);
		
		assertTrue("Expecting a 'p1' followed by 'p2' followed by 'p1' again", fp1 < fp2 && fp2 < sp1);
	}
	
	private int countOccurrencies(String pattern, String in) {
		Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(in);

        int count = 0;
        while (mat.find()) count++;
        return count;
	}
	
	@Test
	public void sameInstanceEmbedded() throws Exception {
		DummyPerson person1 = new DummyPerson();
		DummyPerson person2 = new DummyPerson();
		
		person1.setName("Persona1");
		person2.setName("Persona2");
		
		person1.setIdentifier("p1");
		person2.setIdentifier("p2");

		person2.getFriends().add(person1);

		List<DummyPerson> lst = new ArrayList<DummyPerson>();
		lst.add(person1);
		lst.add(person1);
		lst.add(person2);
		
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("kp1", person1);
		map.put("kp2", person2);
		map.put("kp1_2", person1);
		map.put("kp2_2", person2);
		map.put("kpl", lst);
		map.put("kloop", map);
		map.put("kempty", new ArrayList<Object>());
		
		ObjectMapper om = new ObjectMapper();
		om.setOption(DefaultOptions.INCLUDE_EMPTY, false);
		DummyEntityProvider prov = new DummyEntityProvider();
		om.setEntityNameProvider(prov);
		om.setEntityFactory(prov);
		om.setClassDataFactory(prov);

		{
			String json = null;
			json = om.writeValueAsString(map);
			System.out.println(json);
			// Assert each p1 and p2 are serialized only once
			checkThat(countOccurrencies("\"id\":\"p1\"", json),equalTo(1));
			checkThat(countOccurrencies("\"id\":\"p2\"", json),equalTo(1));
			
			// Assert the others all have only the id
			// p1 is in id:p1 when unrolled, then "p1" in the kpl list, then ["p1"] in friends of p2, then in kp1 and kp1_2
			checkThat(countOccurrencies("\"p1\"", json), equalTo(5));
			// p2 is in id:p2 when unrolled, then in kp2 and kp2_2
			checkThat(countOccurrencies("\"p2\"", json), equalTo(3));

			checkThat(json,containsString("\"kempty\":["));
			checkThat(json,not(containsString("kloop")));
		}
		
		{
			om.setOption(DefaultOptions.UNROLL_OBJECTS, true);
			String json = null;
			json = om.writeValueAsString(map);
			System.out.println(json);			
			checkThat(json,containsString("\"kp2_2\":{"));
			checkThat(json,containsString("\"friends\":[{"));
			checkThat(json,containsString("\"kp1_2\":{"));
			checkThat(json,containsString("\"kp1\":{"));
			checkThat(json,containsString("\"kp2\":{"));
			checkThat(json,containsString("\"kpl\":[{"));
			checkThat(json,containsString("\"kempty\":["));
			checkThat(json,not(containsString("kloop")));
		}
	}

	@Test
	public void objectWithOutFields() throws Exception {
		DummyPerson person = makePerson();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();

		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses.address");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			checkThat(json, containsString("\"name\":"));
			checkThat(json, containsString("\"Simone\""));
			
			checkThat(json, not(containsString("\"surname\":")));
			checkThat(json, not(containsString("\"Gianni\"")));
			
			checkThat(json, containsString("\"addresses\":"));
			
			checkThat(json, containsString("\"address\":"));
			checkThat(json, containsString("\"simoneg@apache.org\""));
			
			checkThat(json, not(containsString("\"type\":")));
			checkThat(json, not(containsString("\"EMAIL\"")));
			checkThat(json, not(containsString("\"places\":")));
			
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			checkThat(json, containsString("\"name\":"));
			checkThat(json, containsString("\"Simone\""));
			
			checkThat(json, not(containsString("\"surname\":")));
			checkThat(json, not(containsString("\"Gianni\"")));
			
			checkThat(json, containsString("\"addresses\":"));
			
			checkThat(json, containsString("\"address\":"));
			checkThat(json, containsString("\"simoneg@apache.org\""));
			
			checkThat(json, containsString("\"type\":"));
			checkThat(json, containsString("\"EMAIL\""));
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("places.address");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			checkThat(json, containsString("\"name\":"));
			checkThat(json, containsString("\"Simone\""));
			
			checkThat(json, not(containsString("\"surname\":")));
			checkThat(json, not(containsString("\"Gianni\"")));

			checkThat(json, containsString("\"places\":"));
		}
		
	}

	@Test
	public void objectWithOutFieldsOverride() throws Exception {
		DummyPerson person = makePerson();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			checkThat(json, not(containsString("Doremi")));
			checkThat(json, not(containsString("addressTag1")));
		}
		{
			OutField of = new OutField(null);
			of.getCreateSub("name");
			of.getCreateSub("addresses.*");
			
			String json = mapper.writeValueAsString(person, of);
			System.out.println(json);
			
			checkThat(json, containsString("Doremi"));
			checkThat(json, containsString("addressTag1"));
		}
		// TODO we could implement this, the double star
		/*
		{
			Map<String,Object> map = new HashMap<>();
			map.put("users", Arrays.asList(person));
			
			OutField of = new OutField(null);
			of.getCreateSub("users.**");
			
			String json = mapper.writeValueAsString(map, of);
			System.out.println(json);
			
			checkThat(json, containsString("addressTag1"));
		}
		*/
	}
	
	@Test
	public void entity() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DummyEntityProvider provider = new DummyEntityProvider();
		mapper.setEntityNameProvider(provider);
		mapper.setEntityFactory(provider);
		mapper.setClassDataFactory(provider);		
		mapper.init();

		DummyPerson person = makePerson();
		
		String json = mapper.writeValueAsString(person, null);
		System.out.println(json);
		
		checkThat(json, containsString("\"@entity\":"));
		checkThat(json, containsString("\"Person\""));

		checkThat(json, containsString("\"@entity\":"));
		checkThat(json, containsString("\"Address\""));
		
		checkThat(json, containsString("\"id\":"));
		checkThat(json, containsString("\"p1\""));
		checkThat(json, containsString("\"a1\""));
		
		checkThat(json, not(containsString("\"identifier\":")));
	}
	
	@Test
	public void includes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		OutField of = OutField.getRoot("inclAlways","inclNotNull","inclNotEmpty","secretByGetter","secretBySetter","hidden1","hidden2");

		// Defaults to off
		mapper.setOption(DefaultOptions.INCLUDE_EMPTY, false);
		mapper.setOption(DefaultOptions.INCLUDE_NULLS, false);
		// all nulls
		{
			DummyAnnotations da = new DummyAnnotations();
			da.setInclAlways(null);
			da.setInclNotEmpty(null);
			da.setInclNotNull(null);
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			checkThat(json, containsString("inclAlways"));
			checkThat(json, not(containsString("inclNotNull")));
			checkThat(json, not(containsString("inclNotEmpty")));
		}
		// all empty
		{
			DummyAnnotations da = new DummyAnnotations();
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			checkThat(json, containsString("inclAlways"));
			checkThat(json, containsString("inclNotNull"));
			checkThat(json, not(containsString("inclNotEmpty")));
		}
		// all with elements
		{
			DummyAnnotations da = new DummyAnnotations();
			da.setInclAlways(Arrays.asList("test"));
			da.setInclNotEmpty(Arrays.asList("test"));
			da.setInclNotNull(Arrays.asList("test"));
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			checkThat(json, containsString("inclAlways"));
			checkThat(json, containsString("inclNotNull"));
			checkThat(json, containsString("inclNotEmpty"));
		}
		// all ignore
		{
			DummyAnnotations da = new DummyAnnotations();
			da.setSecretByGetter("test");
			da.setSecretBySetter("test");
			da.setHidden1("test");
			da.setHidden2("test");
			String json = mapper.writeValueAsString(da, of);
			System.out.println(json);
			checkThat(json, not(containsString("secretByGetter")));
			checkThat(json, not(containsString("secretBySetter")));
			checkThat(json, not(containsString("hidden1")));
			checkThat(json, not(containsString("hidden2")));
		}
	}
	
	@Test
	public void invalidNativeSerialization() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		checkThat(om.writeValueAsString("ciao"), equalTo("ciao"));
		checkThat(om.writeValueAsString(1), equalTo("1"));
		checkThat(om.writeValueAsString(1.0d), equalTo("1.0"));
		checkThat(om.writeValueAsString(true), equalTo("true"));
		checkThat(om.writeValueAsString(null), equalTo("null"));
	}

	@Test
	public void giveOptionSerializesOnDemandOnly() {
		ObjectMapper om = new ObjectMapper();
		om.setOption(DefaultOptions.ALWAYS_SERIALIZE_ON_DEMAND_ONLY, true);

		String json = om.writeValueAsString(new DummyPerson());
		checkThat(json, containsString("places"));
	}
}
