package io.pdef.immutable;

import io.pdef.MethodDescriptor;
import org.junit.Test;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ImmutableMethodDescriptorTest {
	@Test
	public void testGetName() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.TESTINDEX_METHOD;
		assertNotNull(method);
		assertEquals("testIndex", method.getName());
	}

	@Test
	public void testGetExc() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.TESTINDEX_METHOD;
		assertNotNull(method);
		assertTrue(method.getExc() == TestException.DESCRIPTOR);
	}

	@Test
	public void testIndexPostRemote() throws Exception {
		MethodDescriptor<TestInterface, ?> index = TestInterface.TESTINDEX_METHOD;
		MethodDescriptor<TestInterface, ?> remote = TestInterface.TESTREMOTE_METHOD;
		MethodDescriptor<TestInterface, ?> post = TestInterface.TESTPOST_METHOD;
		MethodDescriptor<TestInterface, ?> iface = TestInterface.TESTINTERFACE_METHOD;

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
		MethodDescriptor<TestInterface, ?> method = TestInterface.TESTINDEX_METHOD;
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		method.invoke(object, new Object[] {1, 2});
		verify(object).testIndex(1, 2);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exception() throws Exception {
		MethodDescriptor<TestInterface, ?> method = TestInterface.TESTEXC_METHOD;
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		doThrow(new TestException()).when(object).testExc();

		method.invoke(object, null);
	}
}
