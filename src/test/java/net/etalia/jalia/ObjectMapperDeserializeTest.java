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

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.TypeUtil;
import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Assert;
import org.junit.Test;

public class ObjectMapperDeserializeTest extends TestBase {

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
		checkThat(ret, notNullValue());
		checkThat(ret, instanceOf(Map.class));
		
		Map<String,Object> map = (Map<String, Object>) ret;
		checkThat(map, hasEntry("testString", (Object)"string"));
		checkThat(map, hasEntry("testInt", (Object)1));
		checkThat(map, hasEntry("testBoolean", (Object)true));
		checkThat(map, hasEntry("testLong", (Object)(new Long(-62075462400000l))));
		
		Object subMapObj = map.get("subMap");
		checkThat(subMapObj, notNullValue());
		checkThat(subMapObj, instanceOf(Map.class));
		
		Map<String,String> subMap = (Map<String, String>) subMapObj;
		checkThat(subMap, hasEntry("subString", "subString"));
		
	}
	
	@Test
	public void intMap() throws Exception {
		String json = "{ 'a1' : 1, 'a2' : 2}";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<Map<String,Integer>>() {}.type());
		checkThat(ret, notNullValue());
		checkThat(ret, instanceOf(Map.class));
		
		Map<String,Integer> map = (Map<String, Integer>) ret;
		checkThat(map, hasEntry("a1", 1));
		checkThat(map, hasEntry("a2", 2));
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
		checkThat(ret, notNullValue());
		checkThat(ret, instanceOf(List.class));
		
		List<Object> list = (List<Object>) ret;
		checkThat(list, contains((Object)1l, (Object)1.0d, "a2", true));
	}

	@Test
	public void intList() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<List<Integer>>() {}.type());
		checkThat(ret, notNullValue());
		checkThat(ret, instanceOf(List.class));
		
		List<Integer> list = (List<Integer>) ret;
		checkThat(list, contains(1,2,3));
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
		checkThat(ret, notNullValue());
		checkThat(ret, instanceOf(List.class));
		checkThat(ret, instanceOf(LinkedList.class));
		
		List<Integer> list = (List<Integer>) ret;
		checkThat(list, contains(1,2,3));
	}
	
	@Test
	public void intArray() throws Exception {
		String json = "[ 1, 2, 3]";
		json = replaceQuote(json);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.init();
		
		Object ret = mapper.readValue(json, new TypeUtil.Specific<int[]>() {}.type());
		checkThat(ret, notNullValue());
		checkThat(ret.getClass().isArray(), equalTo(true));
		
		int[] list = (int[]) ret;
		checkThat(list[0], equalTo(1));
		checkThat(list[1], equalTo(2));
		checkThat(list[2], equalTo(3));
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
		
		checkThat(val, notNullValue());
		checkThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		checkThat(person.getName(), equalTo("Mario"));
		checkThat(person.getSurname(), equalTo("Rossi"));
		checkThat(person.getAge(), equalTo(21));
		checkThat(person.getHeight(), equalTo(5.2f));
		checkThat(person.getActive(), equalTo(true));
		checkThat(person.getBirthDay(), notNullValue());
		checkThat(person.getBirthDay().getTime(), equalTo(1000l));
		
		checkThat(person.getAddresses(), hasSize(1));
		checkThat(person.getAddresses().get(0), notNullValue());
		checkThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		checkThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		checkThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
		
		checkThat(person.getTags(), hasSize(2));
		checkThat(person.getTags(), containsInAnyOrder("tag1","tag2"));
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
		
		checkThat(val, notNullValue());
		checkThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		checkThat(person.getName(), equalTo("Mario"));
		checkThat(person.getSurname(), equalTo("Rossi"));
		checkThat(person.getAge(), equalTo(21));
		checkThat(person.getActive(), equalTo(true));
		checkThat(person.getBirthDay(), notNullValue());
		checkThat(person.getBirthDay().getTime(), equalTo(1000l));
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
		
		checkThat(val, notNullValue());
		checkThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		checkThat(person.getName(), equalTo("Mario"));
		checkThat(person.getSurname(), equalTo("Rossi"));
		checkThat(person.getAge(), equalTo(null));
		checkThat(person.getActive(), equalTo(null));
		checkThat(person.getBirthDay(), nullValue());
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
		
		checkThat(val, notNullValue());
		checkThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		checkThat(person.getBirthDay().getTime(), equalTo(289467082000l));
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
		
		checkThat(val, notNullValue());
		checkThat(val, instanceOf(DummyPerson.class));
		
		DummyPerson person = (DummyPerson) val;
		checkThat(person.getName(), equalTo("Simone"));
		checkThat(person.getSurname(), equalTo("Gianni"));
		
		checkThat(person.getAddresses(), hasSize(1));
		checkThat(person.getAddresses().get(0), notNullValue());
		checkThat(person.getAddresses().get(0), instanceOf(DummyAddress.class));
		
		checkThat(person.getAddresses().get(0).getType(), equalTo(AddressType.EMAIL));
		checkThat(person.getAddresses().get(0).getAddress(), equalTo("m.rossi@gmail.com"));
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
		
		checkThat(rperson, sameInstance(person));
		checkThat(rperson.getAddresses(), sameInstance(prelist));
		
		checkThat(prelist, hasSize(3));
		checkThat(prelist.get(0).getIdentifier(), equalTo("a3"));
		checkThat(prelist.get(1), sameInstance(a1));
		checkThat(prelist.get(2), sameInstance(a2));
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
		
		checkThat(rperson, sameInstance(person));
		checkThat(rperson.getTags(), sameInstance(preset));
		
		checkThat(preset, hasSize(3));
		checkThat(preset, containsInAnyOrder("tag1","tag2","tag3"));
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
		
		checkThat(rperson, sameInstance(person));
		checkThat(rperson.getAddresses(), sameInstance(prelist));
		
		System.out.println(rperson);
		
		checkThat(prelist, hasSize(2));
		checkThat(prelist.get(0).getIdentifier(), equalTo("a4"));
		checkThat(prelist.get(1), sameInstance(a1));
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
		
		checkThat(rperson, sameInstance(person));
		checkThat(rperson.getTags(), sameInstance(preset));
		
		checkThat(preset, hasSize(2));
		checkThat(preset, containsInAnyOrder("tag1","tag4"));
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
		checkThat(prelist, hasSize(2));
		checkThat(prelist.get(0).getIdentifier(), equalTo("a1"));
		checkThat(prelist.get(0), sameInstance(a1));
		checkThat(prelist.get(1).getIdentifier(), equalTo("a2"));
		checkThat(prelist.get(1), sameInstance(a2));
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
		checkThat(rperson.getExtraData(), hasEntry("extra1", (Object)"extra"));
		checkThat(rperson.getSecrets(), contains("s1","s2"));
	}

	@Test(expected = JaliaException.class)
	public void cannotChangeMainAddressWithUnfoundOne() throws Exception {
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
						"'type':'EMAIL'," +
						"'address':'a@b.com'" +
						"}"+
						"}";

		om.readValue(json2.replace("'", "\""), DummyPerson.class);
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
			checkThat(rperson.getBestFriend(), notNullValue());
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
			checkThat(rperson.getBestFriend(), nullValue());
		}
	}

	@Test
	public void invalidNativeDeserializations() throws Exception {
		ObjectMapper om = new ObjectMapper();
		
		checkThat(om.readValue("test of string { with \"stuff\" [] }", String.class), equalTo("test of string { with \"stuff\" [] }"));
		checkThat(om.readValue("1", Long.class), equalTo(1l));
		checkThat(om.readValue("1.0", Double.class), equalTo(1.0d));
		checkThat(om.readValue("true", Boolean.class), equalTo(true));
		checkThat(om.readValue("null", Boolean.class), nullValue());
		checkThat(om.readValue("null", DummyPerson.class), nullValue());
	}

	@Test
	public void pollutedDeSerCache() throws Exception {
		DummyEntityProvider ep = new DummyEntityProvider();
		ep.addToDb(new DummyAddress("a4", AddressType.EMAIL, "a@b.com"));
		ObjectMapper om = new ObjectMapper();
		om.setEntityFactory(ep);
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

	@Test
	public void bigDecimal() throws Exception {
		ObjectMapper om = new ObjectMapper();
		String json = "{'@entity':'Person','balance':70000.00}";
		DummyPerson person = om.readValue(json.replace("'", "\""), DummyPerson.class);
		assertTrue(person.getBalance().compareTo(new BigDecimal("70000.00")) == 0);
	}

}
