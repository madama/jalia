package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.etalia.jalia.annotations.JsonAllowEntityPropertyChanges;
import org.junit.Before;
import org.junit.Test;

public class JsonAllowEntityPropertyChangesTest extends TestBase {

    public static class Basic extends DummyEntity {
        private String name;
        private Basic allow;
        private Basic deny;
        private List<Basic> alloweds = new ArrayList<>();
        private List<Basic> denieds = new ArrayList<>();
        private Map<String,Basic> allowMap = new HashMap<>();
        private Map<String,Basic> denyMap = new HashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonAllowEntityPropertyChanges
        public Basic getAllow() {
            return allow;
        }
        public void setAllow(Basic allow) {
            this.allow = allow;
        }

        public Basic getDeny() {
            return deny;
        }
        public void setDeny(Basic deny) {
            this.deny = deny;
        }

        @JsonAllowEntityPropertyChanges
        public List<Basic> getAlloweds() {
            return alloweds;
        }

        public List<Basic> getDenieds() {
            return denieds;
        }

        @JsonAllowEntityPropertyChanges
        public Map<String, Basic> getAllowMap() {
            return allowMap;
        }

        public Map<String, Basic> getDenyMap() {
            return denyMap;
        }
    }

    private final ObjectMapper plain = new ObjectMapper();
    private DummyEntityProvider provider = new DummyEntityProvider();
    private ObjectMapper withFactory = new ObjectMapper();
    private Basic existing;
    private Basic sub;
    private Basic subsub;

    @Before
    public void setupBeans() {
        existing = new Basic();
        existing.setName("base");
        existing.setIdentifier("b");
        sub = new Basic();
        sub.setName("sub");
        sub.setIdentifier("s1");
        subsub = new Basic();
        subsub.setName("subsub");
        subsub.setIdentifier("s2");
        provider.addToDb(existing, sub, subsub);
        withFactory.setEntityFactory(provider);
    }

    private Basic readExisting(ObjectMapper mapper, String jsonLike) {
        return mapper.readValue(jsonLike.replaceAll("'","\""), existing);
    }

    @Test
    public void shouldAllowModificationOnForbiddenPojo() {
        existing.setDeny(sub);

        Basic result = readExisting(plain,"{'name':'newbase','deny':{'name':'newsub'}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getDeny(), sameInstance(sub));
        checkThat(result.getDeny().getName(), equalTo("newsub"));
    }

    @Test
    public void shouldNotAllowModificationOnForbiddenEntity() {
        existing.setDeny(sub);

        Basic result = readExisting(withFactory,
                "{'name':'newbase','deny':{'id':'s1','name':'newsub'}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getDeny(), sameInstance(sub));
        checkThat(result.getDeny().getName(), equalTo("sub"));
    }

    @Test
    public void shouldAllowModificationOnForbiddenEntityIfOptionsSet() {
        existing.setDeny(sub);
        withFactory.setOption(DefaultOptions.ALWAYS_ALLOW_ENTITY_PROPERTY_CHANGES, true);

        Basic result = readExisting(withFactory,
                "{'name':'newbase','deny':{'id':'s1','name':'newsub'}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getDeny(), sameInstance(sub));
        checkThat(result.getDeny().getName(), equalTo("newsub"));
    }


    @Test
    public void shouldAllowModificationOnPojo() {
        existing.setAllow(sub);

        Basic result = readExisting(plain,
                "{'name':'newbase','allow':{'name':'newsub'}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAllow(), sameInstance(sub));
        checkThat(result.getAllow().getName(), equalTo("newsub"));
    }

    @Test
    public void shouldAllowModificationOnEntity() {
        existing.setAllow(sub);

        Basic result = readExisting(withFactory,
                "{'name':'newbase','allow':{'id':'s1','name':'newsub'}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAllow(), sameInstance(sub));
        checkThat(result.getAllow().getName(), equalTo("newsub"));
    }


    @Test
    public void shouldNotPermitChangeOnSubOfAllowedSub() {
        existing.setAllow(sub);
        sub.setDeny(subsub);

        Basic result = readExisting(withFactory,
                "{'name':'newbase','allow':{'id':'s1','name':'newsub', 'deny': {'id':'s2','name':'newsubsub'}}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAllow(), sameInstance(sub));
        checkThat(result.getAllow().getName(), equalTo("newsub"));
        checkThat(result.getAllow().getDeny(), sameInstance(subsub));
        checkThat(result.getAllow().getDeny().getName(), equalTo("subsub"));
    }

    @Test
    public void shouldPermitOnAllowedList() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','alloweds':[{'id':'s1','name':'newsub'}]}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAlloweds(), hasSize(1));
        checkThat(result.getAlloweds().get(0).getName(), equalTo("newsub"));
    }

    @Test
    public void shouldNotPermitOnDeniedList() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','denieds':[{'id':'s1','name':'newsub'}]}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getDenieds(), hasSize(1));
        checkThat(result.getDenieds().get(0), sameInstance(sub));
        checkThat(result.getDenieds().get(0).getName(), equalTo("sub"));
    }

    @Test
    public void shouldNotPermitOnDeniedSubOfAllowedList() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','alloweds':[{'id':'s1','name':'newsub', " +
                        "'denieds':[{'id':'s2','name':'newsubsub'}]}]}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAlloweds(), hasSize(1));
        checkThat(result.getAlloweds().get(0), sameInstance(sub));
        checkThat(result.getAlloweds().get(0).getName(), equalTo("newsub"));
        checkThat(result.getAlloweds().get(0).getDenieds(), hasSize(1));
        checkThat(result.getAlloweds().get(0).getDenieds().get(0), sameInstance(subsub));
        checkThat(result.getAlloweds().get(0).getDenieds().get(0).getName(), equalTo("subsub"));
    }

    @Test
    public void shouldPermitOnAllowedMap() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','allowMap':{'a':{'id':'s1','name':'newsub'}}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAllowMap(), hasKey("a"));
        checkThat(result.getAllowMap().get("a"), sameInstance(sub));
        checkThat(result.getAllowMap().get("a").getName(), equalTo("newsub"));
    }

    @Test
    public void shouldNotPermitOnDeniedMap() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','denyMap':{'a':{'id':'s1','name':'newsub'}}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getDenyMap(), hasKey("a"));
        checkThat(result.getDenyMap().get("a"), sameInstance(sub));
        checkThat(result.getDenyMap().get("a").getName(), equalTo("sub"));
    }

    @Test
    public void shouldNotPermitOnDeniedSubOfAllowedMap() {
        Basic result = readExisting(withFactory,
                "{'name':'newbase','allowMap':{'a':{'id':'s1','name':'newsub', " +
                        "'denyMap':{'a':{'id':'s2','name':'newsubsub'}}}}}");

        checkThat(result, sameInstance(existing));
        checkThat(result.getName(), equalTo("newbase"));
        checkThat(result.getAllowMap(), hasKey("a"));
        checkThat(result.getAllowMap().get("a"), sameInstance(sub));
        checkThat(result.getAllowMap().get("a").getName(), equalTo("newsub"));
        checkThat(result.getAllowMap().get("a").getDenyMap(), hasKey("a"));
        checkThat(result.getAllowMap().get("a").getDenyMap().get("a"), sameInstance(subsub));
        checkThat(result.getAllowMap().get("a").getDenyMap().get("a").getName(), equalTo("subsub"));
    }

}
