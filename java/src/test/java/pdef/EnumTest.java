package pdef;

import pdef.test.TestEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EnumTest {
	@Test
	public void testParse() throws Exception {
		TestEnum enum0 = TestEnum.parse("ThreE");
		assertEquals(TestEnum.THREE, enum0);
	}

	@Test
	public void testParse_null() throws Exception {
		TestEnum enum0 = TestEnum.parse(null);
		assertEquals(TestEnum.ONE, enum0);
	}

	@Test
	public void testSerialize() throws Exception {
		String s = TestEnum.serialize(TestEnum.TWO);
		assertEquals("two", s);
	}

	@Test
	public void testSerialize_null() throws Exception {
		String s = TestEnum.serialize(null);
		assertNull(s);
	}
}
