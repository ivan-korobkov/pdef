package io.pdef.meta;

import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

public class InterfaceTypeTest {
	private InterfaceType<TestInterface> metaType = TestInterface.META_TYPE;

	@Test
	public void testFindMethod() throws Exception {
		InterfaceMethod method = metaType.findMethod("indexMethod");

		assertNotNull(method);
		assertEquals("indexMethod", method.getName());
	}

	@Test
	public void testGetIndexMethod() throws Exception {
		InterfaceMethod method = metaType.getIndexMethod();
		InterfaceMethod expected = metaType.findMethod("indexMethod");
		assertTrue(method == expected);
	}

	@Test
	public void testFindType() throws Exception {
		InterfaceType type = InterfaceType.findMetaType(TestInterface.class);
		assertTrue(type == TestInterface.META_TYPE);
	}

	@Test
	public void testFindType_notFound() throws Exception {
		InterfaceType type = InterfaceType.findMetaType(Runnable.class);
		assertNull(type);
	}
}
