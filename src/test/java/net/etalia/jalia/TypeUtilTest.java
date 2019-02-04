package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import java.util.List;

import net.etalia.jalia.TypeUtil;

import org.junit.Test;

public class TypeUtilTest extends TestBase {

	@Test
	public void simpleClassInspection() throws Exception {
		TypeUtil tu = TypeUtil.get(DummyPerson.class);
		
		checkThat(tu.getConcrete(), equalTo((Class)DummyPerson.class));
		checkThat(tu.isInstantiatable(), equalTo(true));
		
		TypeUtil ret = tu.findReturnTypeOf("getAddresses");
		checkThat(ret, notNullValue());
		checkThat(ret.getConcrete(), equalTo((Class)List.class));
		checkThat(ret.isInstantiatable(), equalTo(false));
		
		TypeUtil getparam = ret.findParameterOf("add", 0);
		checkThat(getparam, notNullValue());
		checkThat(getparam.getConcrete(), equalTo((Class)DummyAddress.class));
	}

}
