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
	public void testValueTypeOf() throws Exception {
		assertEquals(TypeEnum.BOOL, TypeEnum.valueTypeOf(Boolean.class));
		assertEquals(TypeEnum.INT16, TypeEnum.valueTypeOf(Short.class));
		assertEquals(TypeEnum.INT32, TypeEnum.valueTypeOf(Integer.class));
		assertEquals(TypeEnum.INT64, TypeEnum.valueTypeOf(Long.class));
		assertEquals(TypeEnum.FLOAT, TypeEnum.valueTypeOf(Float.class));
		assertEquals(TypeEnum.DOUBLE, TypeEnum.valueTypeOf(Double.class));
		assertEquals(TypeEnum.STRING, TypeEnum.valueTypeOf(String.class));
		assertEquals(TypeEnum.LIST, TypeEnum.valueTypeOf(ArrayList.class));
		assertEquals(TypeEnum.SET, TypeEnum.valueTypeOf(HashSet.class));
		assertEquals(TypeEnum.MAP, TypeEnum.valueTypeOf(HashMap.class));
		assertEquals(TypeEnum.VOID, TypeEnum.valueTypeOf(Void.class));
		assertEquals(TypeEnum.ENUM, TypeEnum.valueTypeOf(TestEnum.class));
		assertEquals(TypeEnum.MESSAGE, TypeEnum.valueTypeOf(TestMessage.class));
		assertEquals(TypeEnum.MESSAGE, TypeEnum.valueTypeOf(TestException.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueTypeOf_unsupported() throws Exception {
		TypeEnum.valueTypeOf(TestInterface.class);
	}

	@Test(expected = NullPointerException.class)
	public void testValueTypeOf_null() throws Exception {
		TypeEnum.valueTypeOf(null);
	}
}
