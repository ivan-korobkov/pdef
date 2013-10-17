package io.pdef.descriptors;

import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

public class InterfaceDescriptorTest {
	private InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;

	@Test
	public void test() throws Exception {
		InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;
		MethodDescriptor<TestInterface, Integer> indexMethod = TestInterface.TESTINDEX_METHOD;

		assertEquals(TestInterface.class, descriptor.getJavaClass());
		assertEquals(TestException.DESCRIPTOR, descriptor.getExc());
		assertEquals(11, descriptor.getMethods().size());
		assertEquals(indexMethod, descriptor.getIndexMethod());
	}

	@Test
	public void testFindMethod() throws Exception {
		MethodDescriptor<TestInterface, ?> expected = TestInterface.TESTINDEX_METHOD;
		MethodDescriptor<TestInterface, ?> method = descriptor.findMethod("testIndex");

		assertEquals(expected, method);
	}

	@Test
	public void testFindDescriptor() throws Exception {
		InterfaceDescriptor descriptor = InterfaceDescriptor.findDescriptor(TestInterface.class);
		assertTrue(descriptor == TestInterface.DESCRIPTOR);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindDescriptor_notFound() throws Exception {
		InterfaceDescriptor.findDescriptor(Runnable.class);
	}
}
