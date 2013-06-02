package io.pdef;

import com.google.common.collect.ImmutableBiMap;
import io.pdef.test.TestEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PdefEnumTest {

	@Test
	public void test() throws Exception {
		Pdef pdef = new Pdef();
		PdefEnum descriptor = new PdefEnum(TestEnum.class, pdef);

		assertTrue(descriptor.isDatatype());
		assertTrue(descriptor.isPrimitive());
	}

	@Test
	public void testGetValues() throws Exception {
		Pdef pdef = new Pdef();
		PdefEnum descriptor = new PdefEnum(TestEnum.class, pdef);

		assertTrue(TestEnum.class == descriptor.getJavaClass());
		assertEquals(ImmutableBiMap.<String, Enum<?>>of("ONE", TestEnum.ONE, "TWO",
				TestEnum.TWO, "THREE", TestEnum.THREE),
				descriptor.getValues());
	}

	/** The first value should be the default one. */
	@Test
	public void testDefaultValue() throws Exception {
		Pdef pdef = new Pdef();
		PdefEnum descriptor = new PdefEnum(TestEnum.class, pdef);

		assertEquals(TestEnum.ONE, descriptor.getDefaultValue());
	}
}
