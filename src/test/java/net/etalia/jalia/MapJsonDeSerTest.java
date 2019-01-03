package net.etalia.jalia;

import net.etalia.jalia.annotations.JsonMap;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MapJsonDeSerTest {

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

        assertThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        assertThat(existing.getData(), not(hasEntry("v1", (Object)"v1")));
        assertThat(existing.getData(), sameInstance(preData));
    }

    @Test
    public void shouldRetainValues() {
        WithRetain existing = new WithRetain();
        Map<String, String> preData = existing.getData();
        existing.getData().put("v1", "v1");

        mapper.readValue("{'data':{'v2':'v2'}}".replace("'","\""), existing, WithRetain.class);

        assertThat(existing.getData(), hasEntry("v2", "v2"));
        assertThat(existing.getData(), hasEntry("v1","v1"));
        assertThat(existing.getData(), sameInstance(preData));
    }

    @Test
    public void shouldInferHintFromType() {
        Basic existing = new Basic();
        Map<String, Object> preData = existing.getData();
        Basic preChild = new Basic();
        preChild.getData().put("c1", "c1");
        existing.getData().put("v1", preChild);

        mapper.readValue("{'data':{'v2':'v2', 'v1': {'data': { 'c2':'c2' }}}}".replace("'","\""),
                existing, Basic.class);

        assertThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        assertThat(existing.getData().get("v1"), notNullValue());
        assertThat(existing.getData().get("v1"), sameInstance((Object)preChild));
        assertThat(((Basic)existing.getData().get("v1")).getData(), hasEntry("c2", (Object)"c2"));
        assertThat(((Basic)existing.getData().get("v1")).getData(), not(hasEntry("c1", (Object)"c1")));
        assertThat(existing.getData(), sameInstance(preData));
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

        assertThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        assertThat(existing.getData().get("v1"), notNullValue());
        assertThat(existing.getData().get("v1"), not(sameInstance((Object)preChild)));
        assertThat(existing.getData(), sameInstance(preData));
    }

    @Test
    public void shouldDropMap() {
        WithDrop existing = new WithDrop();
        Map<String, Object> preData = existing.getData();
        existing.getData().put("v1", "v1");

        mapper.readValue("{'data':{'v2':'v2'}}".replace("'","\""), existing, WithDrop.class);

        assertThat(existing.getData(), hasEntry("v2", (Object)"v2"));
        assertThat(existing.getData(), not(hasEntry("v1", (Object)"v1")));
        assertThat(existing.getData(), not(sameInstance(preData)));
    }
}