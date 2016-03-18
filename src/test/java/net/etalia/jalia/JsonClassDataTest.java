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

public class JsonClassDataTest {

	@Test
	public void gettables() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();
		
		JsonClassData jcd = factory.getClassData(DummyPerson.class, null);
		assertThat(jcd, notNullValue());
		
		JsonClassData jcd2 = factory.getClassData(DummyPerson.class, null);
		assertThat(jcd2, sameInstance(jcd));
		
		assertThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		assertThat(jcd.getGettables(), hasItem("name"));
		assertThat(jcd.getGettables(), hasItem("surname"));
		assertThat(jcd.getGettables(), hasItem("addresses"));
		assertThat(jcd.getGettables(), hasItem("identifier"));
		assertThat(jcd.getGettables(), not(hasItem("class")));
		assertThat(jcd.getGettables(), not(hasItem("password")));
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
		
		assertThat((String)jcd.getValue("name", person), equalTo("Simone"));
		assertThat((String)jcd.getValue("surname", person), equalTo("Gianni"));
	}
	
	@Test
	public void altered() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		DummyEntityProvider prov = new DummyEntityProvider();
		mapper.setEntityFactory(prov);
		mapper.setClassDataFactory(prov);
		JsonContext ctx = new JsonContext(mapper);
		
		JsonClassData jcd = mapper.getClassDataFactory().getClassData(DummyPerson.class, ctx);
		assertThat(jcd, notNullValue());
		
		assertThat(jcd.getGettables(), notNullValue());
		System.out.println(jcd.getGettables());
		
		assertThat(jcd.getGettables(), hasItem("name"));
		assertThat(jcd.getGettables(), hasItem("surname"));
		assertThat(jcd.getGettables(), hasItem("addresses"));
		assertThat(jcd.getGettables(), not(hasItem("class")));
		assertThat(jcd.getGettables(), not(hasItem("identifier")));
	}
	
	@Test
	public void settings() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();		
		
		JsonClassData jcd = factory.getClassData(DummyPerson.class, null);

		DummyPerson person = new DummyPerson();
		jcd.setValue("name", "Simone", person);
		
		assertThat(person.getName(), equalTo("Simone"));
	}

	@Test
	public void annotations() throws Exception {
		JsonClassDataFactory factory = new JsonClassDataFactoryImpl();		
	
		{
			JsonClassData jcd = factory.getClassData(DummyAnnotations.class, null);
			
			assertThat(jcd.getGettables(), containsInAnyOrder("both","getOnly","getOnlyByGetter","unusual","alternative","objBoolean","natBoolean","inclAlways","inclNotNull","inclNotEmpty"));
			assertThat(jcd.getSettables(), containsInAnyOrder("both","setOnly","setOnlyBySetter","unusual","alternative","objBoolean","natBoolean","inclAlways","inclNotNull","inclNotEmpty"));
			assertThat(jcd.getDefaults(), containsInAnyOrder("both"));
			
			assertThat(jcd.getSetHint("alternative").getConcrete(), equalTo((Class)Integer.TYPE));
			
			assertThat(jcd.getOptions("both"), nullValue());
			assertThat(jcd.getOptions("inclAlways"), hasEntry(DefaultOptions.INCLUDE_NULLS.toString(), (Object)true));
			assertThat(jcd.getOptions("inclAlways"), hasEntry(DefaultOptions.INCLUDE_EMPTY.toString(), (Object)true));
		}		
		{
			JsonClassData jcd = factory.getClassData(DummyClassAnnotations.class, null);
			
			assertThat(jcd.getOptions("both"), hasEntry(DefaultOptions.INCLUDE_NULLS.toString(), (Object)true));
			assertThat(jcd.getOptions("both"), hasEntry(DefaultOptions.INCLUDE_EMPTY.toString(), (Object)true));
		}		
		
	}
	
}
