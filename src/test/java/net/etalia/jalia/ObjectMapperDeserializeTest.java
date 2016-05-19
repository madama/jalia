package net.etalia.jalia;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.TypeUtil;
import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperDeserializeTest {

	private String replaceQuote(String json) {
		return json.replace("'", "\"");
	}
	
	@Test
	public void simpleMap() throws Exception {
		String json = "{ 'testString':'string', 'testInt':1, 'testBoolean':true, 'subMap' : { 'subString':'subString' }, 'testNull':null, 'testLong':-62075462400000}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json);
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(Map.class));
		
		Map<String,Object> map = (Map<String, Object>) ret;
		assertThat(map, hasEntry("testString", (Object)"string"));
		assertThat(map, hasEntry("testInt", (Object)1));
		assertThat(map, hasEntry("testBoolean", (Object)true));
		assertThat(map, hasEntry("testLong", (Object)(new Long(-62075462400000l))));
		
		Object subMapObj = map.get("subMap");
		assertThat(subMapObj, notNullValue());
		assertThat(subMapObj, instanceOf(Map.class));
		
		Map<String,String> subMap = (Map<String, String>) subMapObj;
		assertThat(subMap, hasEntry("subString", "subString"));
		
	}
	
	@Test
	public void intMap() throws Exception {
		String json = "{ 'a1' : 1, 'a2' : 2}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<Map<String,Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(Map.class));
		
		Map<String,Integer> map = (Map<String, Integer>) ret;
		assertThat(map, hasEntry("a1", 1));
		assertThat(map, hasEntry("a2", 2));
	}
	
	@Test(expected=JaliaException.class)
	public void intMapError() throws Exception {
		String json = "{ 'a1' : 1, 'a2' : 'ciao'}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		mapper.readValue(json, new TypeUtil.Specific<Map<String,Integer>>() {}.type());
	}

	@Test
	public void simpleList() throws Exception {
		String json = "[ 1, 1.0, 'a2', true]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json);
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		
		List<Object> list = (List<Object>) ret;
		assertThat(list, contains((Object)1l, (Object)1.0d, "a2", true));
	}

	@Test
	public void intList() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<List<Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		
		List<Integer> list = (List<Integer>) ret;
		assertThat(list, contains(1,2,3));
	}
	
	@Test(expected=JaliaException.class)
	public void intListError() throws Exception {
		String json = "[ 1, 2, 'ciao']";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		mapper.readValue(json, new TypeUtil.Specific<List<Integer>>() {}.type());
	}
	
	@Test
	public void intLinkedList() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<LinkedList<Integer>>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret, instanceOf(List.class));
		assertThat(ret, instanceOf(LinkedList.class));
		
		List<Integer> list = (List<Integer>) ret;
		assertThat(list, contains(1,2,3));
	}
	
	@Test
	public void intArray() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<int[]>() {}.type());
		assertThat(ret, notNullValue());
		assertThat(ret.getClass().isArray(), equalTo(true));
		
		int[] list = (int[]) ret;
		assertThat(list[0], equalTo(1));
		assertThat(list[1], equalTo(2));
		assertThat(list[2], equalTo(3));
	}
	
	
	@Test
	public void simpleEntity() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'age':21," +
					"'height':5.2," +
					"'active':true," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]," +
					"'tags':[" +
						"'tag1'," +
						"'tag2'" +
					"]," +
					"'birthDay':1000" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""));
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Mario"));
		assertThat(person.getSurname(), equalTo("Rossi"));
		assertThat(person.getAge(), equalTo(21));
		assertThat(person.getHeight(), equalTo(5.2f));
		assertThat(person.getActive(), equalTo(true));
		assertThat(person.getBirthDay(), notNullValue());
		assertThat(person.getBirthDay().getTime(), equalTo(1000l));
		
		assertThat(person.getAddresses(), hasSize(1));
		assertThat(person.getAddresses().get(0), notNullValue());
		assertThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		assertThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		assertThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
		
		assertThat(person.getTags(), hasSize(2));
		assertThat(person.getTags(), containsInAnyOrder("tag1","tag2"));
	}

	@Test
	public void simpleEntityWithStrings() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'age':'21'," +
					"'active':'true'," +
					"'birthDay':'1000'" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""));
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Mario"));
		assertThat(person.getSurname(), equalTo("Rossi"));
		assertThat(person.getAge(), equalTo(21));
		assertThat(person.getActive(), equalTo(true));
		assertThat(person.getBirthDay(), notNullValue());
		assertThat(person.getBirthDay().getTime(), equalTo(1000l));
	}

	@Test
	public void simpleEntityWithEmptyStrings() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'age':''," +
					"'active':''," +
					"'birthDay':''" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""));
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Mario"));
		assertThat(person.getSurname(), equalTo("Rossi"));
		assertThat(person.getAge(), equalTo(null));
		assertThat(person.getActive(), equalTo(null));
		assertThat(person.getBirthDay(), nullValue());
	}
	
	@Test
	public void simpleEntityWithISO8601Date() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'birthDay':'1979-03-05T07:31:22Z'" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		Object val = om.readValue(json.replace("'", "\""));
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getBirthDay().getTime(), equalTo(289467082000l));
	}
	@Test
	public void entityFromExisting() throws Exception {
		String json = 
				"{" +
					"'id':'p1'," +
					"'@entity':'Person'," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(new DummyPerson("p1", "Simone","Gianni"));
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);		
		om.init();
		Object val = om.readValue(json.replace("'", "\""));
		
		assertThat(val, notNullValue());
		assertThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		assertThat(person.getName(), equalTo("Simone"));
		assertThat(person.getSurname(), equalTo("Gianni"));
		
		assertThat(person.getAddresses(), hasSize(1));
		assertThat(person.getAddresses().get(0), notNullValue());
		assertThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		assertThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		assertThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
	}
	
	@Test(expected=JaliaException.class)
	public void wrongHint() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'addresses':[" +
						"{" +
							"'type':'EMAIL'," +
							"'address':'m.rossi@gmail.com'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		om.readValue(json.replace("'", "\""), new TypeUtil.Specific<Integer>(){}.type());
	}
	
	@Test(expected=JaliaException.class)
	public void wrongInnerType() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'name':'Mario',"+
					"'surname':'Rossi'," +
					"'addresses':[" +
						"{" +
							"'@entity':'Person'," +
							"'name':'wrong'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		om.readValue(json.replace("'", "\""));
	}
	
	@Test
	public void exceptionMessage() throws Exception {
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'addresses':[" +
						"{" +
							"'type':'INVALID_TYPE'" +
						"}"+
					"]" +
				"}";
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(new DummyEntityProvider());
		om.init();
		try {
			om.readValue(json.replace("'", "\""));
			Assert.fail("Should throw exception");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void differentEntitiesInList() throws Exception {
		DummyAddress a1 = new DummyAddress("a1",AddressType.EMAIL, "simoneg@apache.org");
		DummyAddress a2 = new DummyAddress("a2",AddressType.HOME, "Via Prove, 21");
		
		DummyPerson person = new DummyPerson("p1","Simone","Gianni",a1,a2);
		List<DummyAddress> prelist = person.getAddresses();
		
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(person,a1,a2);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'addresses':[" +
						"{" +
							"'@entity':'Address'," +
							"'id':'a3'," +
							"'type':'EMAIL'," +
							"'address':'a@b.com'" +
						"}"+
						"," +
						"{" +
							"'@entity':'Address'," +
							"'id':'a1'" +
						"}" +
						"," +
						"{" +
							"'@entity':'Address'," +
							"'id':'a2'" +
						"}" +
					"]" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		
		assertThat(rperson, sameInstance(person));
		assertThat(rperson.getAddresses(), sameInstance(prelist));
		
		assertThat(prelist, hasSize(3));
		assertThat(prelist.get(0).getIdentifier(), equalTo("a3"));
		assertThat(prelist.get(1), sameInstance(a1));
		assertThat(prelist.get(2), sameInstance(a2));
	}

	@Test
	public void differentEntitiesInSet() throws Exception {
		DummyPerson person = new DummyPerson("p1","Simone","Gianni");
		person.initTags("tag1","tag2");
		Set<String> preset = person.getTags();
		
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(person);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'tags':[" +
						"'tag3'," +
						"'tag1'," +
						"'tag2'" +
					"]" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		
		assertThat(rperson, sameInstance(person));
		assertThat(rperson.getTags(), sameInstance(preset));
		
		assertThat(preset, hasSize(3));
		assertThat(preset, containsInAnyOrder("tag1","tag2","tag3"));
	}
	
	@Test
	public void lessEntitiesInList() throws Exception {
		DummyAddress a1 = new DummyAddress("a1",AddressType.EMAIL, "simoneg@apache.org");
		DummyAddress a2 = new DummyAddress("a2",AddressType.HOME, "Via Prove, 21");
		DummyAddress a3 = new DummyAddress("a3",AddressType.OFFICE, "Via del Lavoro, 21");
		
		DummyPerson person = new DummyPerson("p1","Simone","Gianni",a1,a2,a3);
		List<DummyAddress> prelist = person.getAddresses();
		
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(person,a1,a2);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);		
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'addresses':[" +
						"{" +
							"'@entity':'Address'," +
							"'id':'a4'," +
							"'type':'EMAIL'," +
							"'address':'a@b.com'" +
						"}"+
						"," +
						"{" +
							"'@entity':'Address'," +
							"'id':'a1'" +
						"}" +
					"]" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		
		assertThat(rperson, sameInstance(person));
		assertThat(rperson.getAddresses(), sameInstance(prelist));
		
		System.out.println(rperson);
		
		assertThat(prelist, hasSize(2));
		assertThat(prelist.get(0).getIdentifier(), equalTo("a4"));
		assertThat(prelist.get(1), sameInstance(a1));
	}

	@Test
	public void lessEntitiesInSet() throws Exception {
		DummyPerson person = new DummyPerson("p1","Simone","Gianni");
		person.initTags("tag1","tag2","tag3");
		Set<String> preset = person.getTags();
		
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(person);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);		
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'tags':[" +
						"'tag1',"+
						"'tag4'"+
					"]" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		
		assertThat(rperson, sameInstance(person));
		assertThat(rperson.getTags(), sameInstance(preset));
		
		assertThat(preset, hasSize(2));
		assertThat(preset, containsInAnyOrder("tag1","tag4"));
	}
	
	@Test
	public void embeddedEntities() throws Exception {
		DummyAddress a1 = new DummyAddress("a1",AddressType.EMAIL, "simoneg@apache.org");
		DummyAddress a2 = new DummyAddress("a2",AddressType.HOME, "Via Prove, 21");
		DummyAddress a3 = new DummyAddress("a3",AddressType.OFFICE, "Via del Lavoro, 21");
		
		DummyEntityProvider provider = new DummyEntityProvider();
		provider.addToDb(a1,a2,a3);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);		
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'addresses':[" +
						"\"a1\",\"a2\"" +
					"]" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		List<DummyAddress> prelist = rperson.getAddresses();
		assertThat(prelist, hasSize(2));
		assertThat(prelist.get(0).getIdentifier(), equalTo("a1"));
		assertThat(prelist.get(0), sameInstance(a1));
		assertThat(prelist.get(1).getIdentifier(), equalTo("a2"));
		assertThat(prelist.get(1), sameInstance(a2));
	}
	
	@Test
	public void unmodifiables() throws Exception {
		DummyEntityProvider provider = new DummyEntityProvider();
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);		
		om.init();
		
		String json = 
				"{" +
					"'@entity':'Person'," +
					"'id':'p1'," +
					"'secrets':[" +
						"'s1','s2'" +
					"]," +
					"'extraData':{" +
						"'extra1':'extra'" +
					"}" +
				"}";
		
		Object rpersonObj = om.readValue(json.replace("'", "\""));
		DummyPerson rperson = (DummyPerson) rpersonObj;
		assertThat(rperson.getExtraData(), hasEntry("extra1", (Object)"extra"));
		assertThat(rperson.getSecrets(), contains("s1","s2"));
	}

	@Test
	public void nullBeans() throws Exception {
		DummyPerson p1 = new DummyPerson();
		p1.setIdentifier("p1");
		
		DummyPerson bf = new DummyPerson();
		bf.setIdentifier("pbf");
		p1.setBestFriend(bf);
		
		DummyEntityProvider provider = new DummyEntityProvider();
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(provider);
		om.setEntityFactory(provider);
		om.setClassDataFactory(provider);
		
		provider.addToDb(p1);
		
		om.init();

		{
			String json = 
					"{" +
						"'@entity':'Person'," +
						"'id':'p1'" +
					"}";
			
			Object rpersonObj = om.readValue(json.replace("'", "\""));
			DummyPerson rperson = (DummyPerson) rpersonObj;
			assertThat(rperson.getBestFriend(), notNullValue());
		}
		{
			String json = 
					"{" +
						"'@entity':'Person'," +
						"'id':'p1'," +
						"'bestFriend':null" +
					"}";
			
			Object rpersonObj = om.readValue(json.replace("'", "\""));
			DummyPerson rperson = (DummyPerson) rpersonObj;
			assertThat(rperson.getBestFriend(), nullValue());
		}
	}

	@Test
	public void invalidNativeDeserializations() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		assertThat(om.readValue("test of string { with \"stuff\" [] }", String.class), equalTo("test of string { with \"stuff\" [] }"));
		assertThat(om.readValue("1", Long.class), equalTo(1l));
		assertThat(om.readValue("1.0", Double.class), equalTo(1.0d));
		assertThat(om.readValue("true", Boolean.class), equalTo(true));
		assertThat(om.readValue("null", Boolean.class), nullValue());
		assertThat(om.readValue("null", DummyPerson.class), nullValue());
	}
	
	
	@Test
	public void pollutedDeSerCache() throws Exception {
		ObjectMapper om = new ObjectMapper();
		String json = 
				"{" +
						"'@entity':'Person'," +
						"'mainAddress':null" +
				"}";
		om.readValue(json.replace("'", "\""), DummyPerson.class);
		
		
		String json2 = 
				"{" +
						"'@entity':'Person'," +
						"'mainAddress':" +
							"{" +
								"'@entity':'Address'," +
								"'id':'a4'," +
								"'type':'EMAIL'," +
								"'address':'a@b.com'" +
							"}"+
				"}";
		
		om.readValue(json2.replace("'", "\""), DummyPerson.class);
	}
	
}
