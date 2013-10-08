package io.pdef.types;

import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class InterfaceTypeTest {
	private InterfaceType descriptor = TestInterface.TYPE;

	@Test
	public void testGetMethods() throws Exception {
		List<InterfaceMethod> methods = descriptor.getMethods();
	}

	@Test
	public void testFindMethod() throws Exception {
		InterfaceMethod method = descriptor.findMethod("indexMethod");

		assertNotNull(method);
		assertEquals("indexMethod", method.getName());
	}

	@Test
	public void testGetIndexMethod() throws Exception {
		InterfaceMethod method = descriptor.getIndexMethod();
		InterfaceMethod expected = descriptor.findMethod("indexMethod");
		assertTrue(method == expected);
	}

	@Test
	public void testFindDescriptor() throws Exception {
		InterfaceType descriptor = InterfaceType.findDescriptor(TestInterface.class);
		assertTrue(descriptor == TestInterface.TYPE);
	}

	@Test
	public void testFindDescriptor_notFound() throws Exception {
		InterfaceType descriptor = InterfaceType.findDescriptor(Runnable.class);
		assertNull(descriptor);
	}
}
