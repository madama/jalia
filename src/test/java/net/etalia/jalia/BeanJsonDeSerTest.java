package net.etalia.jalia;

import net.etalia.jalia.annotations.JsonIgnore;
import org.junit.Test;

public class BeanJsonDeSerTest {

    public static class TestGetterGivingError {

        @JsonIgnore
        public String getSomething() {
            throw new IllegalStateException("Cannot read this");
        }

        public void setSomething(String value) {

        }
    }

    @Test
    public void deserialize_shouldNotCallGetterOfIgnores() {
        ObjectMapper om = new ObjectMapper();
        om.readValue("{'something':'test'}".replaceAll("'", "\""), TestGetterGivingError.class);
    }

}
