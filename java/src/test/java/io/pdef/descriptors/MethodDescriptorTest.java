package io.pdef.descriptors;

import org.junit.Test;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MethodDescriptorTest {
	@Test
	public void testGetName() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.INDEXMETHOD_METHOD;
		assertNotNull(method);
		assertEquals("indexMethod", method.getName());
	}

	@Test
	public void testGetExc() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.INDEXMETHOD_METHOD;
		assertNotNull(method);
		assertTrue(method.getExc() == TestException.DESCRIPTOR);
	}

	@Test
	public void testIndexPostRemote() throws Exception {
		MethodDescriptor<TestInterface, ?> index = TestInterface.INDEXMETHOD_METHOD;
		MethodDescriptor<TestInterface, ?> remote = TestInterface.REMOTEMETHOD_METHOD;
		MethodDescriptor<TestInterface, ?> post = TestInterface.POSTMETHOD_METHOD;
		MethodDescriptor<TestInterface, ?> iface = TestInterface.INTERFACEMETHOD_METHOD;

		assertTrue(index.isIndex());
		assertTrue(index.isRemote());
		assertFalse(index.isPost());

		assertFalse(remote.isIndex());
		assertTrue(remote.isRemote());
		assertFalse(remote.isPost());

		assertFalse(post.isIndex());
		assertTrue(post.isRemote());
		assertTrue(post.isPost());

		assertFalse(iface.isIndex());
		assertFalse(iface.isRemote());
		assertFalse(iface.isPost());
	}

	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.INDEXMETHOD_METHOD;
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		method.invoke(object, new Object[] {1, 2});
		verify(object).indexMethod(1, 2);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exception() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.EXCMETHOD_METHOD;
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		doThrow(new TestException()).when(object).excMethod();

		method.invoke(object, null);
	}
}
