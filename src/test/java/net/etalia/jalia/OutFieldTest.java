package net.etalia.jalia;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Map;
import java.util.Set;

import net.etalia.jalia.OutField;

import org.junit.Test;

public class OutFieldTest extends TestBase {

	@Test
	public void testParse() {
		OutField root = new OutField(null, "");
		
		String[] subs = new String[] {
				"id",
				"gallery.id",
				"gallery.title",
				"author.profile.picture",
				"author.profile.id",
				"author.email",
				"author.email.host"
		};
		
		for (String sub : subs) {
			root.getCreateSub(sub);
		}
		
		Map<String, OutField> rsubs = root.getSubs();
		checkThat(rsubs.keySet(), hasSize(3));
		checkThat(rsubs, hasKey("id"));
		checkThat(rsubs, hasKey("gallery"));
		checkThat(rsubs, hasKey("author"));
		
		OutField gitem = rsubs.get("gallery");
		Map<String, OutField> gsubs = gitem.getSubs();
		checkThat(gsubs.keySet(), hasSize(2));
		checkThat(gsubs, hasKey("id"));
		checkThat(gsubs, hasKey("title"));
		
		OutField aitem = rsubs.get("author");
		Map<String, OutField> asubs = aitem.getSubs();
		checkThat(asubs.keySet(), hasSize(2));
		checkThat(asubs, hasKey("profile"));
		checkThat(asubs, hasKey("email"));

		OutField apitem = asubs.get("profile");
		checkThat(apitem.getFullPath(), equalTo("author.profile"));
		Map<String, OutField> apsubs = apitem.getSubs();
		checkThat(apsubs.keySet(), hasSize(2));
		checkThat(apsubs, hasKey("id"));
		checkThat(apsubs, hasKey("picture"));

		for (String sub : subs) {
			checkThat("Not found " + sub, root.getSub(sub), notNullValue());
		}
		
		checkThat(root.getSub("pippo"), nullValue());
		checkThat(root.getSub("pippo.pluto"), nullValue());
		checkThat(root.getSub("gallery.pippo"), nullValue());
		
		Set<String> stringList = root.toStringList();
		for (String sub : subs) {
			checkThat(stringList, hasItem(sub));
		}
		// There are 3 more subs : gallery, author and author.profile 
		checkThat(stringList, hasSize(subs.length + 3));
	}

	@Test
	public void shouldParseGroupsJsonSimple() {
		String groupJson = "{'group':\n // comment\n ['prop1','prop2','link1.prop1','link2.*']}".replace("'", "\"");
		OutField.getGroups().clear();
		OutField.parseGroupsJson(new StringReader(groupJson));

		checkThat(OutField.getGroups(), hasKey("group"));
		OutField group = OutField.getGroups().get("group");
		Set<String> stringList = group.toStringList();
		checkThat(stringList, containsInAnyOrder("prop1", "prop2", "link1", "link1.prop1", "link2"));
	}

	@Test
	public void shouldParseGroupsJsonObject() {
		String groupJson = (
				"{'group': " +
					"{'prop1': true,'prop2':true," +
					"'link1': {'prop1':true}, " +
					"'link2': '*', " +
					"'link3': ['p3','p4']}" +
				"}").replace("'","\"");
		OutField.getGroups().clear();
		OutField.parseGroupsJson(new StringReader(groupJson));

		checkThat(OutField.getGroups(), hasKey("group"));
		OutField group = OutField.getGroups().get("group");
		Set<String> stringList = group.toStringList();
		checkThat(stringList, containsInAnyOrder("prop1", "prop2", "link1", "link1.prop1", "link2", "link3",
				"link3.p3", "link3.p4"));
	}

	@Test
	public void shouldParseGroupsMixedArrays() {
		String groupJson = (
				"{'group': [" +
						"['prop1','prop2']," +
						"{'link1': ['prop1','prop2']}, " +
						"'link2.*,link3' " +
						"]}").replace("'","\"");
		OutField.getGroups().clear();
		OutField.parseGroupsJson(new StringReader(groupJson));

		checkThat(OutField.getGroups(), hasKey("group"));
		OutField group = OutField.getGroups().get("group");
		Set<String> stringList = group.toStringList();
		checkThat(stringList, containsInAnyOrder("prop1", "prop2", "link1", "link1.prop1", "link1.prop2", "link2", "link3"));
	}
}
