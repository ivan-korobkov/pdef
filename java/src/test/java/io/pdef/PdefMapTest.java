package io.pdef;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;

public class PdefMapTest {
	public Map<String, Integer> mapField;

	@Test
	public void test() throws Exception {
		Pdef pdef = new Pdef();
		PdefMap descriptor = new PdefMap(getMapType(), pdef);

		assertTrue(descriptor.isDatatype());
		assertFalse(descriptor.isPrimitive());
	}

	@Test
	public void testGetKey() throws Exception {
		Pdef pdef = new Pdef();
		PdefMap descriptor = new PdefMap(getMapType(), pdef);

		assertEquals(pdef.get(String.class), descriptor.getKey());
	}

	@Test
	public void testGetValue() throws Exception {
		Pdef pdef = new Pdef();
		PdefMap descriptor = new PdefMap(getMapType(), pdef);

		assertEquals(pdef.get(Integer.class), descriptor.getValue());
	}

	@Test
	public void testGetDefaultValue() throws Exception {
		Pdef pdef = new Pdef();
		PdefMap descriptor = new PdefMap(getMapType(), pdef);

		assertTrue(descriptor.getDefaultValue() == ImmutableMap.of());
	}

	Type getMapType() {
		try {
			return PdefMapTest.class.getField("mapField").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
