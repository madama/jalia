package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import java.io.StringReader;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;

import org.junit.Test;

public class JsonReaderTest extends TestBase {

	@Test
	public void test() throws Exception {
		String json = "{ 'a1':1, 'a2':'a2', 's1' : { 'sa1':1, 'sa2':'a2', 'saa': [ 1,2,3 ]}, 'a3':'a3'}";
		json = json.replace("'", "\"");
		
		StringReader sr = new StringReader(json);
		JsonReader jr = new JsonReader(sr);
		
		// Try super early fork
		{
			JsonReader la = jr.lookAhead();
			la.beginObject();
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("a1"));
			checkThat(la.nextInt(), equalTo(1));
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("a2"));
			checkThat(la.nextString(), equalTo("a2"));
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("s1"));
			checkThat(la.peek(), equalTo(JsonToken.BEGIN_OBJECT));
			la.skipValue();
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("a3"));
			checkThat(la.nextString(), equalTo("a3"));
			la.endObject();
			
			la.close();
		}
		
		// Try read some stuff
		jr.beginObject();
		checkThat(jr.hasNext(), equalTo(true));
		checkThat(jr.nextName(), equalTo("a1"));
		checkThat(jr.nextInt(), equalTo(1));
		
		// Try intermediate fork
		{
			JsonReader la = jr.lookAhead();
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("a2"));
			checkThat(la.nextString(), equalTo("a2"));
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("s1"));
			checkThat(la.peek(), equalTo(JsonToken.BEGIN_OBJECT));
			la.skipValue();
			checkThat(la.hasNext(), equalTo(true));
			checkThat(la.nextName(), equalTo("a3"));
			checkThat(la.nextString(), equalTo("a3"));
			la.endObject();
			
			la.close();
		}

		// Go on
		checkThat(jr.hasNext(), equalTo(true));
		checkThat(jr.nextName(), equalTo("a2"));
		checkThat(jr.nextString(), equalTo("a2"));
	}

}
