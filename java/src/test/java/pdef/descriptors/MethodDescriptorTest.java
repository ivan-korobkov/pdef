package pdef.descriptors;

import org.junit.Test;
import pdef.test.interfaces.TestException;
import pdef.test.interfaces.TestInterface;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MethodDescriptorTest {
	private final InterfaceDescriptor iface = TestInterface.DESCRIPTOR;

	@Test
	public void testGetName() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertEquals("indexMethod", method.getName());
	}

	@Test
	public void testGetExc() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.getExc() == iface.getExc());
	}

	@Test
	public void testIsIndex() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.isIndex());
	}

	@Test
	public void testIsPost() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertFalse(method.isPost());
	}

	@Test
	public void testIsRemote() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assertNotNull(method);
		assertTrue(method.isRemote());
	}

	@Test
	public void testIsRemote_false() throws Exception {
		MethodDescriptor method = iface.findMethod("interfaceMethod");
		assertNotNull(method);
		assertFalse(method.isRemote());
	}

	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor method = iface.findMethod("indexMethod");
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		method.invoke(object, new Object[] {1, 2});
		verify(object).indexMethod(1, 2);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exception() throws Exception {
		MethodDescriptor method = iface.findMethod("excMethod");
		assert method != null;

		TestInterface object = mock(TestInterface.class);
		doThrow(TestException.builder().build()).when(object).excMethod();

		method.invoke(object, null);
	}
}
