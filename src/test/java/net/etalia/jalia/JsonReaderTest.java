package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import java.io.StringReader;

import net.etalia.jalia.stream.JsonReader;
import net.etalia.jalia.stream.JsonToken;

import org.junit.Test;

public class JsonReaderTest {

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
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("a1"));
			assertThat(la.nextInt(), equalTo(1));
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("a2"));
			assertThat(la.nextString(), equalTo("a2"));
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("s1"));
			assertThat(la.peek(), equalTo(JsonToken.BEGIN_OBJECT));
			la.skipValue();
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("a3"));
			assertThat(la.nextString(), equalTo("a3"));
			la.endObject();
			
			la.close();
		}
		
		// Try read some stuff
		jr.beginObject();
		assertThat(jr.hasNext(), equalTo(true));
		assertThat(jr.nextName(), equalTo("a1"));
		assertThat(jr.nextInt(), equalTo(1));
		
		// Try intermediate fork
		{
			JsonReader la = jr.lookAhead();
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("a2"));
			assertThat(la.nextString(), equalTo("a2"));
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("s1"));
			assertThat(la.peek(), equalTo(JsonToken.BEGIN_OBJECT));
			la.skipValue();
			assertThat(la.hasNext(), equalTo(true));
			assertThat(la.nextName(), equalTo("a3"));
			assertThat(la.nextString(), equalTo("a3"));
			la.endObject();
			
			la.close();
		}

		// Go on
		assertThat(jr.hasNext(), equalTo(true));
		assertThat(jr.nextName(), equalTo("a2"));
		assertThat(jr.nextString(), equalTo("a2"));
	}

}
