package io.pdef.invoke;

import io.pdef.test.interfaces.NextTestInterface;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.util.List;

public class InvocationTest {
	@Test
	public void testConstructor() throws Exception {
		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_wrongMethodArgs() throws Exception {
		Invocation.root(TestInterface.INDEXMETHOD_METHOD, new Object[]{1, 2, 3, 4});
	}

	@Test
	public void testConstructor_copyArgs() throws Exception {
		SimpleMessage message = new SimpleMessage()
				.setABool(true)
				.setAString("hello")
				.setAnInt16((short) -16);

		Invocation invocation = Invocation.root(TestInterface.MESSAGEMETHOD_METHOD,
				new Object[]{message});

		assertEquals(message, invocation.getArgs()[0]);
		assertTrue(message != invocation.getArgs()[0]);
	}

	@Test
	public void testGetMethod() throws Exception {
		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});
		assertEquals(TestInterface.INDEXMETHOD_METHOD, invocation.getMethod());
	}

	@Test
	public void testGetResult() throws Exception {
		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});
		assertEquals(TestInterface.INDEXMETHOD_METHOD.getResult(), invocation.getResult());
	}

	@Test
	public void testGetExc() throws Exception {
		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});
		assertEquals(TestInterface.INDEXMETHOD_METHOD.getExc(), invocation.getExc());
	}

	@Test
	public void testIsRemote() throws Exception {
		Invocation indexInvocation = Invocation
				.root(TestInterface.INDEXMETHOD_METHOD, new Object[]{1, 2});
		Invocation interfaceInvocation = Invocation.root(
				TestInterface.INTERFACEMETHOD_METHOD, new Object[]{1, 2});

		assertFalse(interfaceInvocation.isRemote());
		assertTrue(indexInvocation.isRemote());
	}

	@Test
	public void testToChain() throws Exception {
		List<Invocation> chain = Invocation
				.root(TestInterface.INTERFACEMETHOD_METHOD, new Object[]{1, 2})
				.next(NextTestInterface.STRINGMETHOD_METHOD, new Object[]{"hello"})
				.toChain();

		assertEquals(2, chain.size());
	}

	@Test
	public void testInvoke() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		when(iface.indexMethod(1, 2)).thenReturn(3);

		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});
		InvocationResult result = invocation.invoke(iface);
		assertEquals(3, result.getData());
	}

	@Test
	public void testInvoke_chained() throws Exception {
		TestInterface iface = mock(TestInterface.class, RETURNS_DEEP_STUBS);
		when(iface.interfaceMethod(1, 2).stringMethod("hello")).thenReturn("good bye");

		Invocation invocation = Invocation
				.root(TestInterface.INTERFACEMETHOD_METHOD, new Object[]{1, 2})
				.next(NextTestInterface.STRINGMETHOD_METHOD, new Object[]{"hello"});

		InvocationResult result = invocation.invoke(iface);
		assertEquals("good bye", result.getData());
	}

	@Test
	public void testInvoke_exc() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		doThrow(new TestException()).when(iface).excMethod();
		Invocation invocation = Invocation.root(TestInterface.EXCMETHOD_METHOD, new Object[]{});

		InvocationResult result = invocation.invoke(iface);
		assertEquals(new TestException(), result.getExc());
	}

	@Test
	public void testInvokeSingle() throws Throwable {
		TestInterface iface = mock(TestInterface.class);
		when(iface.indexMethod(1, 2)).thenReturn(3);
		Invocation invocation = Invocation.root(TestInterface.INDEXMETHOD_METHOD,
				new Object[]{1, 2});

		Object result = invocation.invokeSingle(iface);
		assertEquals(3, result);
	}
}
