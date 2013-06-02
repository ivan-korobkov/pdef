package io.pdef;

import com.google.common.collect.ImmutableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

public class PdefListTest {
	public List<String> listField;

	@Test
	public void test() throws Exception {
		Pdef pdef = new Pdef();
		PdefList descriptor = new PdefList(getListType(), pdef);

		assertTrue(descriptor.isDatatype());
		assertFalse(descriptor.isPrimitive());
	}

	@Test
	public void testGetElement() throws Exception {
		Pdef pdef = new Pdef();
		PdefList descriptor = new PdefList(getListType(), pdef);

		assertEquals(getListType(), descriptor.getJavaType());
		assertEquals(pdef.get(String.class), descriptor.getElement());
	}

	@Test
	public void testDefaultValue() throws Exception {
		Pdef pdef = new Pdef();
		PdefList descriptor = new PdefList(getListType(), pdef);
		List<Object> dv = descriptor.getDefaultValue();
		assertTrue(dv == ImmutableList.of());
	}

	Type getListType() {
		try {
			return PdefListTest.class.getField("listField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
