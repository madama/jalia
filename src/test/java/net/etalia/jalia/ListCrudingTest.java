package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Test;

public class ListCrudingTest {

	@Test
	public void test() {
		DummyPerson mainp = new DummyPerson();
		mainp.setIdentifier("mp");
		
		
		DummyPerson f1 = new DummyPerson("f1","Friend","1");
		DummyPerson f2 = new DummyPerson("f2","Friend","2");
		DummyPerson f3 = new DummyPerson("f3","Friend","3");
		mainp.getFriends().add(f1);
		mainp.getFriends().add(f2);
		mainp.getFriends().add(f3);
		
		DummyEntityProvider ep = new DummyEntityProvider();
		ep.addToDb(mainp, f1, f2, f3);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(ep);
		om.setEntityFactory(ep);
		
		System.out.println(om.writeValueAsString(mainp, OutField.getRoot("friends")));
		
		String listCrudding = "" +
				"{'@entity':'Person','id':'mp'," +
				"'friends':[" +
					"{" +
						"'@entity':'Person','id':'f3'," +
						"'name':'Fr3'" +
					"}" +
					"," +
					"{" +
						"'@entity':'Person','id':'f1'" +
					"}" +
					"," +
					"{" +
						"'@entity':'Person',"+
						"'name':'New','surname':'Justmet'" +
					"}" +
				"]}" +
				"";

		DummyPerson nmp = om.readValue(listCrudding.replace('\'', '"'), DummyPerson.class);
		assertThat(nmp, sameInstance(mainp));
		
		System.out.println(om.writeValueAsString(nmp, OutField.getRoot("friends")));
		
		assertThat(mainp.getFriends(), hasSize(3));
		assertThat(mainp.getFriends().get(0).getIdentifier(), equalTo("f3"));
		assertThat(mainp.getFriends().get(0).getName(), equalTo("Fr3"));
		assertThat(mainp.getFriends().get(1).getIdentifier(), equalTo("f1"));
		assertThat(mainp.getFriends().get(1).getName(), equalTo("Friend"));
		assertThat(mainp.getFriends().get(2).getIdentifier(), nullValue());
		assertThat(mainp.getFriends().get(2).getName(), equalTo("New"));
		assertThat(mainp.getFriends().get(2).getSurname(), equalTo("Justmet"));
		
	}
	
	@Test
	public void notOnRootOrSubBeans() throws Exception {
		DummyPerson df = new DummyPerson("p2", "Edmondo","Amicis");
		DummyPerson dp = new DummyPerson("p1", "Smone", "Gianni");
		dp.setBestFriend(df);
		
		DummyEntityProvider ep = new DummyEntityProvider();
		ep.addToDb(df,dp);
		
		ObjectMapper om = new ObjectMapper();
		om.setEntityNameProvider(ep);
		om.setEntityFactory(ep);
		
		String update = "" +
				"{" +
					"'@entity':'Person'," +
					"'name':'Simone'," +
					"'bestFriend':{" +
						"'surname':'De Amicis'" +
					"}" +
				"}" +
				"";
		
		DummyPerson readp = om.readValue(update.replace('\'', '"'), dp, DummyPerson.class);
		assertThat(readp, sameInstance(dp));
		assertThat(readp.getBestFriend(), sameInstance(df));
		assertThat(readp.getIdentifier(), equalTo("p1"));
		assertThat(readp.getBestFriend().getIdentifier(), equalTo("p2"));
	}
}
