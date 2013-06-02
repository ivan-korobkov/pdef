package io.pdef;

import com.google.common.collect.ImmutableSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Set;

public class PdefSetTest {
	public Set<String> setField;

	@Test
	public void test() throws Exception {
		Pdef pdef = new Pdef();
		PdefSet descriptor = new PdefSet(getSetType(), pdef);

		assertTrue(descriptor.isDatatype());
		assertFalse(descriptor.isPrimitive());
	}

	@Test
	public void testGetElement() throws Exception {
		Pdef pdef = new Pdef();
		PdefSet descriptor = new PdefSet(getSetType(), pdef);

		assertEquals(getSetType(), descriptor.getJavaType());
		assertEquals(pdef.get(String.class), descriptor.getElement());
	}

	@Test
	public void testDefaultValue() throws Exception {
		Pdef pdef = new Pdef();
		PdefSet descriptor = new PdefSet(getSetType(), pdef);
		Set<Object> dv = descriptor.getDefaultValue();
		assertTrue(dv == ImmutableSet.of());
	}

	Type getSetType() {
		try {
			return PdefSetTest.class.getField("setField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
