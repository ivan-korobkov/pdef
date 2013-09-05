package pdef.descriptors;

import org.junit.Test;
import pdef.test.interfaces.TestInterface;

import java.util.List;

import static org.junit.Assert.*;

public class InterfaceDescriptorTest {
	private InterfaceDescriptor descriptor = TestInterface.DESCRIPTOR;

	@Test
	public void testGetMethods() throws Exception {
		List<MethodDescriptor> methods = descriptor.getMethods();
	}

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
