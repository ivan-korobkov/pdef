package io.pdef.immutable;

import io.pdef.Descriptors;
import io.pdef.InterfaceDescriptor;
import io.pdef.MethodDescriptor;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

public class ImmutableInterfaceDescriptorTest {
	private InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;

	@Test
	public void test() throws Exception {
		InterfaceDescriptor<TestInterface> descriptor = TestInterface.DESCRIPTOR;
		MethodDescriptor<TestInterface, ?> indexMethod = indexMethod();

		assertEquals(TestInterface.class, descriptor.getJavaClass());
		assertEquals(TestException.DESCRIPTOR, descriptor.getExc());
		assertEquals(11, descriptor.getMethods().size());
		assertEquals(indexMethod, descriptor.getIndexMethod());
	}

	@Test
	public void testFindMethod() throws Exception {
		MethodDescriptor<TestInterface, ?> expected = indexMethod();
		MethodDescriptor<TestInterface, ?> method = descriptor.getMethod("testIndex");

		assertEquals(expected, method);
	}

	@Test
	public void testFindDescriptor() throws Exception {
		InterfaceDescriptor descriptor = Descriptors
				.findInterfaceDescriptor(TestInterface.class);
		assertTrue(descriptor == TestInterface.DESCRIPTOR);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindDescriptor_notFound() throws Exception {
		Descriptors.findInterfaceDescriptor(Runnable.class);
	}

	private MethodDescriptor<TestInterface, ?> indexMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testIndex");
	}
}
