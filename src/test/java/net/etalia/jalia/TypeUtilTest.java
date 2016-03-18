package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import java.util.List;

import net.etalia.jalia.TypeUtil;

import org.junit.Test;

public class TypeUtilTest {

	@Test
	public void simpleClassInspection() throws Exception {
		TypeUtil tu = TypeUtil.get(DummyPerson.class);
		
		assertThat(tu.getConcrete(), equalTo((Class)DummyPerson.class));
		assertThat(tu.isInstantiatable(), equalTo(true));
		
		TypeUtil ret = tu.findReturnTypeOf("getAddresses");
		assertThat(ret, notNullValue());
		assertThat(ret.getConcrete(), equalTo((Class)List.class));
		assertThat(ret.isInstantiatable(), equalTo(false));
		
		TypeUtil getparam = ret.findParameterOf("add", 0);
		assertThat(getparam, notNullValue());
		assertThat(getparam.getConcrete(), equalTo((Class)DummyAddress.class));
	}

}
