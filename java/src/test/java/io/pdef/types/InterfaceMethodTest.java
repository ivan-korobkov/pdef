package io.pdef.types;

import org.junit.Test;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InterfaceMethodTest {
	private final InterfaceType iface = TestInterface.TYPE;

	@Test
	public void testGetName() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertEquals("indexMethod", method.name());
	}

	@Test
	public void testGetExc() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.exc() == iface.getExc());
	}

	@Test
	public void testIsIndex() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.isIndex());
	}

	@Test
	public void testIsPost() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertFalse(method.isPost());
	}

	@Test
	public void testIsRemote() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.isRemote());
	}

	@Test
	public void testIsRemote_false() throws Exception {
		InterfaceMethod method = iface.findMethod("interfaceMethod");
		assertNotNull(method);
		assertFalse(method.isRemote());
	}

	@Test
	public void testInvoke() throws Exception {
		InterfaceMethod method = iface.findMethod("indexMethod");
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		method.invoke(object, new Object[] {1, 2});
		verify(object).indexMethod(1, 2);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exception() throws Exception {
		InterfaceMethod method = iface.findMethod("excMethod");
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		doThrow(new TestException()).when(object).excMethod();

		method.invoke(object, null);
	}
}
