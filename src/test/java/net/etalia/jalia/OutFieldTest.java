package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import net.etalia.jalia.OutField;

import org.junit.Test;

public class OutFieldTest {

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
		assertThat(rsubs.keySet(), hasSize(3));
		assertThat(rsubs, hasKey("id"));
		assertThat(rsubs, hasKey("gallery"));
		assertThat(rsubs, hasKey("author"));	
		
		OutField gitem = rsubs.get("gallery");
		Map<String, OutField> gsubs = gitem.getSubs();
		assertThat(gsubs.keySet(), hasSize(2));
		assertThat(gsubs, hasKey("id"));
		assertThat(gsubs, hasKey("title"));
		
		OutField aitem = rsubs.get("author");
		Map<String, OutField> asubs = aitem.getSubs();
		assertThat(asubs.keySet(), hasSize(2));
		assertThat(asubs, hasKey("profile"));
		assertThat(asubs, hasKey("email"));

		OutField apitem = asubs.get("profile");
		assertThat(apitem.getFullPath(), equalTo("author.profile"));
		Map<String, OutField> apsubs = apitem.getSubs();
		assertThat(apsubs.keySet(), hasSize(2));
		assertThat(apsubs, hasKey("id"));
		assertThat(apsubs, hasKey("picture"));

		for (String sub : subs) {
			assertThat("Not found " + sub, root.getSub(sub), notNullValue());
		}
		
		assertThat(root.getSub("pippo"), nullValue());		
		assertThat(root.getSub("pippo.pluto"), nullValue());		
		assertThat(root.getSub("gallery.pippo"), nullValue());		
		
		Set<String> stringList = root.toStringList();
		System.out.println(stringList);
		for (String sub : subs) {
			assertThat(stringList, hasItem(sub));
		}
		// There are 3 more subs : gallery, author and author.profile 
		assertThat(stringList, hasSize(subs.length + 3));
	}

}
