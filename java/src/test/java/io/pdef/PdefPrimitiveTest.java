package io.pdef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PdefPrimitiveTest {
	@Test
	public void test() throws Exception {
		Pdef pdef = new Pdef();
		PdefPrimitive descriptor = new PdefPrimitive(PdefType.INT32, int.class, 0, pdef);

		assertTrue(descriptor.isDatatype());
		assertTrue(descriptor.isPrimitive());
		assertEquals(int.class, descriptor.getJavaClass());
		assertEquals(0, descriptor.getDefaultValue());
	}
}
