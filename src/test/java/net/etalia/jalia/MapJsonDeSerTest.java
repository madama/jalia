package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.HashMap;
import java.util.Map;
import net.etalia.jalia.annotations.JsonAllowNewInstances;
import net.etalia.jalia.annotations.JsonMap;
import org.junit.Before;
import org.junit.Test;

public class MapJsonDeSerTest extends TestBase {

    private ObjectMapper mapper;

    public static class Basic {
        Map<String, Object> data = new HashMap<>();

        public Map<String, Object> getData() {
            return data;
        }
    }

    public static class WithRetain {
        Map<String, String> data = new HashMap<>();

        @JsonMap(retain=true)
        public Map<String, String> getData() {
            return data;
        }
    }

    public static class WithClear {
        Map<String, Object> data = new HashMap<>();

        @JsonMap(clear=true)
        public Map<String, Object> getData() {
            return data;
        }
    }

    public static class WithDrop {
        Map<String, Object> data = new HashMap<>();

        @JsonMap(drop=true)
        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }

    public static class Chained {
        String name;
        Map<String, Chained> data = new HashMap<>();

        @JsonAllowNewInstances
        public Map<String, Chained> getData() {
            return data;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class SimpleNameProvider implements EntityNameProvider {

        @Override
        public String getEntityName(Class<?> clazz) {
            return clazz.getSimpleName();
        }

        @Override
        public Class<?> getEntityClass(String name) {
            return null;
        }
    }

    @Before
    public void setupObjectMapper() {
        mapper = new ObjectMapper();
        mapper.setEntityNameProvider(new SimpleNameProvider());

    }

    @Test
    public void shouldAddRemoveOnDefault() {
        Basic existing = new Basic();
        Map<String, Object> preData = existing.getData();
        existing.getData().put("v1", "v1");

        mapper.readValue("{'data':{'v2':'v2'}}".replace("'","\""), existing, Basic.class);

        checkThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        checkThat(existing.getData(), not(hasEntry("v1", (Object)"v1")));
        checkThat(existing.getData(), sameInstance(preData));
    }

    @Test
    public void shouldRetainValues() {
        WithRetain existing = new WithRetain();
        Map<String, String> preData = existing.getData();
        existing.getData().put("v1", "v1");

        mapper.readValue("{'data':{'v2':'v2'}}".replace("'","\""), existing, WithRetain.class);

        checkThat(existing.getData(), hasEntry("v2", "v2"));
        checkThat(existing.getData(), hasEntry("v1","v1"));
        checkThat(existing.getData(), sameInstance(preData));
    }

    @Test
    public void shouldInferHintFromMapDeclaration() {
        Chained existing = new Chained();
        mapper.readValue("{'data':{'v1': {'name': 'test'}}}".replace("'","\""),
                existing, Chained.class);

        checkThat(existing.getData().get("v1"), notNullValue());
        checkThat(existing.getData().get("v1").getName(), equalTo("test"));
    }

    @Test
    public void shouldIgnoreInferredHintIfError() {
        Basic existing = new Basic();
        existing.getData().put("v1", "ciao");
        mapper.readValue("{'data':{'v1': {'name': 'test'}}}".replace("'", "\""),
                existing);

        checkThat(existing.getData().get("v1"), notNullValue());
        checkThat(existing.getData().get("v1"), instanceOf(Map.class));
    }

    @Test
    public void shouldPopulatePreviouslyEmptyObject() {
        Basic existing = new Basic();
        mapper.readValue("{'data':{'v1': {}, 'v2':'ciao'}}".replace("'", "\""), existing);
        mapper.readValue("{'data':{'v1': {'name': 'test'}}}".replace("'", "\""), existing);

        checkThat(existing.getData().get("v1"), notNullValue());
        checkThat(existing.getData().get("v1"), instanceOf(Map.class));
        checkThat(((Map) existing.getData().get("v1")).get("name"), equalTo((Object) "test"));
    }

    @Test
    public void shouldClearMap() {
        WithClear existing = new WithClear();
        Map<String, Object> preData = existing.getData();
        Basic preChild = new Basic();
        preChild.getData().put("c1", "c1");
        existing.getData().put("v1", preChild);

        mapper.readValue("{'data':{'v2':'v2', 'v1': {'data': { 'c2':'c2' }}}}".replace("'","\""),
                existing, WithClear.class);

        checkThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        checkThat(existing.getData().get("v1"), notNullValue());
        checkThat(existing.getData().get("v1"), not(sameInstance((Object)preChild)));
        checkThat(existing.getData(), sameInstance(preData));

        ChangeRecorder.Change<Map<String, Object>> change = mapper.getChangeRecorder().getChange(existing, "data");
        checkThat(change, notNullValue());
        checkThat(change.getOldValue().keySet(), hasItems("v1"));
        checkThat(change.getNewValue().keySet(), hasItems("v1", "v2"));
    }

    @Test
    public void shouldDropMap() {
        WithDrop existing = new WithDrop();
        Map<String, Object> preData = existing.getData();
        existing.getData().put("v1", "v1");

        mapper.readValue("{'data':{'v2':'v2'}}".replace("'","\""), existing, WithDrop.class);

        checkThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        checkThat(existing.getData(), not(hasEntry("v1", (Object)"v1")));
        checkThat(existing.getData(), not(sameInstance(preData)));
    }

    @Test
    public void shouldDeserializeMixedStuff() {
        Basic existing = new Basic();
        Map<String, Object> preData = existing.getData();

        mapper.readValue("{'data':{'int':1, 'long': 12345678910100, 'string': 'abc'}}".replace("'", "\""), existing,
                Basic.class);

        checkThat(existing.getData(), hasEntry("int", (Object) 1));
        checkThat(existing.getData(), hasEntry("long", (Object) 12345678910100L));
        checkThat(existing.getData(), hasEntry("string", (Object) "abc"));
    }
}