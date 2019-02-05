package net.etalia.jalia;

import static org.hamcrest.Matchers.*;
import net.etalia.jalia.annotations.JsonAllowNewInstances;
import net.etalia.jalia.annotations.JsonAllowEntityPropertyChanges;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonAllowNewInstancesTest extends TestBase {

    public static class AllowDeny {
        private String name;
        private AllowDeny allow;
        private AllowDeny deny;
        private List<AllowDeny> alloweds = new ArrayList<>();
        private List<AllowDeny> denieds = new ArrayList<>();
        private Map<String, AllowDeny> allowMap = new HashMap<>();
        private Map<String, AllowDeny> denyMap = new HashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonAllowEntityPropertyChanges
        public AllowDeny getAllow() {
            return allow;
        }

        @JsonAllowNewInstances
        public void setAllow(AllowDeny allow) {
            this.allow = allow;
        }

        @JsonAllowEntityPropertyChanges
        public AllowDeny getDeny() {
            return deny;
        }

        public void setDeny(AllowDeny deny) {
            this.deny = deny;
        }

        @JsonAllowNewInstances
        public List<AllowDeny> getAlloweds() {
            return alloweds;
        }

        public List<AllowDeny> getDenieds() {
            return denieds;
        }

        @JsonAllowNewInstances
        public Map<String, AllowDeny> getAllowMap() {
            return allowMap;
        }

        public Map<String, AllowDeny> getDenyMap() {
            return denyMap;
        }
    }


    @Test
    public void shouldPermitNewEntity() {
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','allow':{'name':'new'}}".replaceAll("'","\""),
                AllowDeny.class);
        checkThat(obj, notNullValue());
        checkThat(obj.name, equalTo("test"));
        checkThat(obj.getAllow(), notNullValue());
        checkThat(obj.getAllow().getName(), equalTo("new"));
    }

    @Test
    public void shouldNotPermitNewEntity() {
        expect(JaliaException.class);
        expectMessage("Error reading [deny]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','deny':{'name':'new'}}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldNotPermitNewEntityWithFactory() {
        expect(JaliaException.class);
        expectMessage("Error reading [deny]");
        ObjectMapper om = new ObjectMapper();
        om.setEntityFactory(Mockito.mock(EntityFactory.class));
        AllowDeny obj = om.readValue(
                "{'name':'test','deny':{'name':'new'}}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldNotPermitNewEntityOnSubOfAllowedSub() {
        expect(JaliaException.class);
        expectMessage("Error reading [allow, deny]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','allow':{'name':'new', 'deny': { 'name':'subsub'}}}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldPermitNewEntityOnPermittedList() {
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','alloweds':[{'name':'new'}]}".replaceAll("'","\""),
                AllowDeny.class);
        checkThat(obj, notNullValue());
        checkThat(obj.name, equalTo("test"));
        checkThat(obj.getAlloweds(), hasSize(1));
        checkThat(obj.getAlloweds().get(0).getName(), equalTo("new"));
    }

    @Test
    public void shouldNotPermitNewEntityOnForbiddenList() {
        expect(JaliaException.class);
        expectMessage("Error reading [denieds]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','denieds': { 'name':'subsub'}}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldNotPermitNewEntityOnSubOfAllowedList() {
        expect(JaliaException.class);
        expectMessage("Error reading [alloweds, deny]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','alloweds':[{'name':'new', 'deny': { 'name':'subsub'}}]}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldPermitNewEntityOnPermittedMap() {
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','allowMap':{'a1':{'name':'new'}}}".replaceAll("'","\""),
                AllowDeny.class);
        checkThat(obj, notNullValue());
        checkThat(obj.name, equalTo("test"));
        checkThat(obj.getAllowMap(), hasKey("a1"));
        checkThat(obj.getAllowMap().get("a1").getName(), equalTo("new"));
    }

    @Test
    public void shouldNotPermitNewEntityOnForbiddenMap() {
        expect(JaliaException.class);
        expectMessage("Error reading [denyMap, a1]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','denyMap': {'a1': { 'name':'subsub'}}}".replaceAll("'","\""),
                AllowDeny.class);
    }

    @Test
    public void shouldNotPermitNewEntityOnSubOfAllowedMap() {
        expect(JaliaException.class);
        expectMessage("Error reading [allowMap, a1, deny]");
        ObjectMapper om = new ObjectMapper();
        AllowDeny obj = om.readValue(
                "{'name':'test','allowMap': {'a1': { 'name':'sub', 'deny': {'name':'subsub'}}}}".replaceAll("'","\""),
                AllowDeny.class);
    }

}
