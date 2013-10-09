package io.pdef.types;

import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class InterfaceTypeTest {
	private InterfaceType type = TestInterface.TYPE;

	@Test
	public void testGetMethods() throws Exception {
		List<InterfaceMethod> methods = type.getMethods();
	}

	@Test
	public void testFindMethod() throws Exception {
		InterfaceMethod method = type.findMethod("indexMethod");

		assertNotNull(method);
		assertEquals("indexMethod", method.name());
	}

	@Test
	public void testGetIndexMethod() throws Exception {
		InterfaceMethod method = type.getIndexMethod();
		InterfaceMethod expected = type.findMethod("indexMethod");
		assertTrue(method == expected);
	}

	@Test
	public void testFindType() throws Exception {
		InterfaceType type = InterfaceType.findType(TestInterface.class);
		assertTrue(type == TestInterface.TYPE);
	}

	@Test
	public void testFindType_notFound() throws Exception {
		InterfaceType type = InterfaceType.findType(Runnable.class);
		assertNull(type);
	}
}
