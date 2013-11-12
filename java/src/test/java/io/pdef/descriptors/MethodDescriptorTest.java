package io.pdef.descriptors;

import org.junit.Test;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MethodDescriptorTest {
	@Test
	public void testGetName() throws Exception {
		MethodDescriptor<TestInterface, ?> method = method();
		assertNotNull(method);
		assertEquals("method", method.getName());
	}

	@Test
	public void testGetExc() throws Exception {
		MethodDescriptor<TestInterface, ?> method = method();
		assertNotNull(method);
		assertTrue(method.getExc() == TestException.DESCRIPTOR);
	}

	@Test
	public void testIndexPostTerminal() throws Exception {
		MethodDescriptor<TestInterface, ?> index = method();
		MethodDescriptor<TestInterface, ?> query = TestInterface.DESCRIPTOR.getMethod(
				"query");
		MethodDescriptor<TestInterface, ?> post = TestInterface.DESCRIPTOR.getMethod(
				"post");
		MethodDescriptor<TestInterface, ?> iface = TestInterface.DESCRIPTOR.getMethod(
				"interface0");

		assertTrue(index.isTerminal());
		assertFalse(index.isPost());

		assertTrue(query.isTerminal());
		assertFalse(query.isPost());

		assertTrue(post.isTerminal());
		assertTrue(post.isPost());

		assertFalse(iface.isTerminal());
		assertFalse(iface.isPost());
	}

	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor<TestInterface, ?> method = method();
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		method.invoke(object, new Object[] {1, 2});
		verify(object).method(1, 2);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exception() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.DESCRIPTOR.getMethod(
				"exc0");
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		doThrow(new TestException()).when(object).exc0();

		method.invoke(object, null);
	}

	private MethodDescriptor<TestInterface, ?> method() {
		return TestInterface.DESCRIPTOR.getMethod("method");
	}
}
