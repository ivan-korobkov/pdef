package io.pdef.descriptors;

import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

public class InterfaceDescriptorTest {
	private InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;

	@Test
	public void testFindMethod() throws Exception {
		MethodDescriptor method = descriptor.findMethod("indexMethod");

		assertNotNull(method);
		assertEquals("indexMethod", method.getName());
	}

	@Test
	public void testGetIndexMethod() throws Exception {
		MethodDescriptor method = descriptor.getIndexMethod();
		MethodDescriptor expected = descriptor.findMethod("indexMethod");
		assertTrue(method == expected);
	}

	@Test
	public void testFindDescriptor() throws Exception {
		InterfaceDescriptor descriptor = InterfaceDescriptor.findDescriptor(TestInterface.class);
		assertTrue(descriptor == TestInterface.DESCRIPTOR);
	}

	@Test
	public void testFindDescriptor_notFound() throws Exception {
		InterfaceDescriptor descriptor = InterfaceDescriptor.findDescriptor(Runnable.class);
		assertNull(descriptor);
	}
}
