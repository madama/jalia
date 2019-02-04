package net.etalia.jalia;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import net.etalia.jalia.JsonClassData;
import net.etalia.jalia.JsonContext;
import net.etalia.jalia.ObjectMapper;
import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Before;
import org.junit.Test;

public class JsonClassDataTest extends TestBase {

	@Test
	public void gettables() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();
		
		JsonClassData jcd = factory.getClassData(DummyPerson.class, null);
		checkThat(jcd, notNullValue());
		
		JsonClassData jcd2 = factory.getClassData(DummyPerson.class, null);
		checkThat(jcd2, sameInstance(jcd));
		
		checkThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		checkThat(jcd.getGettables(), hasItem("name"));
		checkThat(jcd.getGettables(), hasItem("surname"));
		checkThat(jcd.getGettables(), hasItem("addresses"));
		checkThat(jcd.getGettables(), hasItem("identifier"));
		checkThat(jcd.getGettables(), not(hasItem("class")));
		checkThat(jcd.getGettables(), not(hasItem("password")));
	}
	
	@Test
	public void getting() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();
		
		JsonClassData jcd = factory.getClassData(DummyPerson.class, null);

		DummyPerson person = new DummyPerson();
		person.setName("Simone");
		person.setSurname("Gianni");
		
		DummyAddress address = new DummyAddress(null, AddressType.EMAIL, "simoneg@apache.org");
		person.getAddresses().add(address);
		
		checkThat((String)jcd.getValue("name", person), equalTo("Simone"));
		checkThat((String)jcd.getValue("surname", person), equalTo("Gianni"));
	}
	
	@Test
	public void altered() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DummyEntityProvider prov = new DummyEntityProvider();
		mapper.setEntityFactory(prov);
		mapper.setClassDataFactory(prov);
		JsonContext ctx = new JsonContext(mapper);
		
		JsonClassData jcd = mapper.getClassDataFactory().getClassData(DummyPerson.class, ctx);
		checkThat(jcd, notNullValue());
		
		checkThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		checkThat(jcd.getGettables(), hasItem("name"));
		checkThat(jcd.getGettables(), hasItem("surname"));
		checkThat(jcd.getGettables(), hasItem("addresses"));
		checkThat(jcd.getGettables(), not(hasItem("class")));
		checkThat(jcd.getGettables(), not(hasItem("identifier")));
	}
	
	@Test
	public void settings() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();
		
		JsonClassData jcd = factory.getClassData(DummyPerson.class, null);

		DummyPerson person = new DummyPerson();
		jcd.setValue("name", "Simone", person);
		
		checkThat(person.getName(), equalTo("Simone"));
	}

	@Test
	public void annotations() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();
	
		{
			JsonClassData jcd = factory.getClassData(DummyAnnotations.class, null);
			
			checkThat(jcd.getGettables(), containsInAnyOrder("both","getOnly","getOnlyByGetter","unusual","alternative","objBoolean","natBoolean","inclAlways","inclNotNull","inclNotEmpty"));
			checkThat(jcd.getSettables(), containsInAnyOrder("both","setOnly","setOnlyBySetter","unusual","alternative","objBoolean","natBoolean","inclAlways","inclNotNull","inclNotEmpty"));
			checkThat(jcd.getDefaults(), containsInAnyOrder("both"));
			
			checkThat(jcd.getSetHint("alternative").getConcrete(), equalTo((Class)Integer.TYPE));
			
			checkThat(jcd.getOptions("both"), nullValue());
			checkThat(jcd.getOptions("inclAlways"), hasEntry(DefaultOptions.INCLUDE_NULLS.toString(), (Object)true));
			checkThat(jcd.getOptions("inclAlways"), hasEntry(DefaultOptions.INCLUDE_EMPTY.toString(), (Object)true));
		}		
		{
			JsonClassData jcd = factory.getClassData(DummyClassAnnotations.class, null);
			
			checkThat(jcd.getGettables(), containsInAnyOrder("both","getOnly","getOnlyByGetter","unusual","alternative","objBoolean","natBoolean","inclAlways","inclNotNull","inclNotEmpty"));
			checkThat(jcd.getOptions("both"), hasEntry(DefaultOptions.INCLUDE_NULLS.toString(), (Object)true));
			checkThat(jcd.getOptions("both"), hasEntry(DefaultOptions.INCLUDE_EMPTY.toString(), (Object)true));
		}		
		
	}
	
}
