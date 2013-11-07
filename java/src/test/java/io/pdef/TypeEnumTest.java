package io.pdef;

import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestEnum;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TypeEnumTest {
	@Test
	public void testDataTypeOf() throws Exception {
		assertEquals(TypeEnum.BOOL, TypeEnum.dataTypeOf(Boolean.class));
		assertEquals(TypeEnum.INT16, TypeEnum.dataTypeOf(Short.class));
		assertEquals(TypeEnum.INT32, TypeEnum.dataTypeOf(Integer.class));
		assertEquals(TypeEnum.INT64, TypeEnum.dataTypeOf(Long.class));
		assertEquals(TypeEnum.FLOAT, TypeEnum.dataTypeOf(Float.class));
		assertEquals(TypeEnum.DOUBLE, TypeEnum.dataTypeOf(Double.class));
		assertEquals(TypeEnum.STRING, TypeEnum.dataTypeOf(String.class));
		assertEquals(TypeEnum.LIST, TypeEnum.dataTypeOf(ArrayList.class));
		assertEquals(TypeEnum.SET, TypeEnum.dataTypeOf(HashSet.class));
		assertEquals(TypeEnum.MAP, TypeEnum.dataTypeOf(HashMap.class));
		assertEquals(TypeEnum.VOID, TypeEnum.dataTypeOf(Void.class));
		assertEquals(TypeEnum.ENUM, TypeEnum.dataTypeOf(TestEnum.class));
		assertEquals(TypeEnum.MESSAGE, TypeEnum.dataTypeOf(TestMessage.class));
		assertEquals(TypeEnum.EXCEPTION, TypeEnum.dataTypeOf(TestException.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDataTypeOf_unsupported() throws Exception {
		TypeEnum.dataTypeOf(TestInterface.class);
	}

	@Test(expected = NullPointerException.class)
	public void testDataTypeOf_null() throws Exception {
		TypeEnum.dataTypeOf(null);
	}
}
