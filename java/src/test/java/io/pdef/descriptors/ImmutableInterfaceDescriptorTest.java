package io.pdef.descriptors;

import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

public class ImmutableInterfaceDescriptorTest {
	private InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;

	@Test
	public void test() throws Exception {
		InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;
		assertEquals(TestInterface.class, descriptor.getJavaClass());
		assertEquals(TestException.DESCRIPTOR, descriptor.getExc());
		assertEquals(10, descriptor.getMethods().size());
	}

	@Test
	public void testFindDescriptor() throws Exception {
		InterfaceDescriptor descriptor = Descriptors.findInterfaceDescriptor(TestInterface.class);
		assertTrue(descriptor == TestInterface.DESCRIPTOR);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindDescriptor_notFound() throws Exception {
		Descriptors.findInterfaceDescriptor(Runnable.class);
	}

	private MethodDescriptor<TestInterface, ?> method() {
		return TestInterface.DESCRIPTOR.getMethod("method");
	}
}
