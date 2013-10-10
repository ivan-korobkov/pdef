package io.pdef.invoke;

import io.pdef.meta.InterfaceMethod;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;
import io.pdef.meta.InterfaceType;
import io.pdef.test.interfaces.NextTestInterface;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;

import java.util.List;

public class InvocationTest {
	private final InterfaceType iface = TestInterface.META_TYPE;
	private final InterfaceMethod excMethod = iface.findMethod("excMethod");
	private final InterfaceMethod indexMethod = iface.findMethod("indexMethod");
	private final InterfaceMethod interfaceMethod = iface.findMethod( "interfaceMethod");
	private final InterfaceMethod nextStringMethod = NextTestInterface.META_TYPE
			.findMethod("stringMethod");

	@Test
	public void testIsRoot() throws Exception {
		Invocation root = Invocation.root();
		Invocation next = root.next(indexMethod, new Object[]{null, null});

		assertTrue(root.isRoot());
		assertFalse(next.isRoot());
	}

	@Test
	public void testGetArgs() throws Exception {
		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testGetMethod() throws Exception {
		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		assertEquals(indexMethod, invocation.getMethod());
	}

	@Test
	public void testGetResult() throws Exception {
		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		assertEquals(indexMethod.result(), invocation.getResult());
	}

	@Test
	public void testGetExc() throws Exception {
		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		assertEquals(indexMethod.exc(), invocation.getExc());
	}

	@Test
	public void testIsRemote() throws Exception {
		Invocation root = Invocation.root();
		Invocation indexInvocation = root.next(indexMethod, new Object[]{1, 2});
		Invocation interfaceInvocation = root.next(interfaceMethod, new Object[]{1, 2});

		assertFalse(root.isRemote());
		assertFalse(interfaceInvocation.isRemote());
		assertTrue(indexInvocation.isRemote());
	}

	@Test
	public void testToChain() throws Exception {
		List<Invocation> chain = Invocation.root()
				.next(interfaceMethod, new Object[]{1, 2})
				.next(nextStringMethod, new Object[]{"hello"})
				.toChain();

		assertEquals(2, chain.size());
	}

	@Test
	public void testInvoke() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		when(iface.indexMethod(1, 2)).thenReturn(3);

		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		InvocationResult result = invocation.invoke(iface);
		assertEquals(3, result.getData());
	}

	@Test
	public void testInvoke_chained() throws Exception {
		TestInterface iface = mock(TestInterface.class, RETURNS_DEEP_STUBS);
		when(iface.interfaceMethod(1, 2).stringMethod("hello")).thenReturn("good bye");

		Invocation invocation = Invocation.root()
				.next(interfaceMethod, new Object[]{1, 2})
				.next(nextStringMethod, new Object[]{"hello"});

		InvocationResult result = invocation.invoke(iface);
		assertEquals("good bye", result.getData());
	}

	@Test
	public void testInvoke_exc() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		doThrow(new TestException()).when(iface).excMethod();

		Invocation invocation = Invocation.root()
				.next(excMethod, new Object[]{});

		InvocationResult result = invocation.invoke(iface);
		assertEquals(new TestException(), result.getData());
	}

	@Test
	public void testInvokeSingle() throws Throwable {
		TestInterface iface = mock(TestInterface.class);
		when(iface.indexMethod(1, 2)).thenReturn(3);

		Invocation invocation = Invocation.root().next(indexMethod, new Object[]{1, 2});
		Object result = invocation.invokeSingle(iface);
		assertEquals(3, result);
	}
}
