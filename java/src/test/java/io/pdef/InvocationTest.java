package io.pdef;

import io.pdef.descriptors.MethodDescriptor;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.util.List;

public class InvocationTest {
	@Test
	public void testConstructor() throws Exception {
		Invocation invocation = Invocation.root(method(), new Object[]{1, 2});
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_wrongMethodArgs() throws Exception {
		Invocation.root(method(), new Object[]{1, 2, 3, 4});
	}

	@Test
	public void testConstructor_copyArgs() throws Exception {
		TestMessage message = new TestMessage()
				.setBool0(true)
				.setString0("hello")
				.setInt0(-16);
		Invocation invocation = Invocation.root(messageMethod(), new Object[]{message});

		assertEquals(message, invocation.getArgs()[0]);
		assertTrue(message != invocation.getArgs()[0]);
	}

	@Test
	public void testGetMethod() throws Exception {
		Invocation invocation = Invocation.root(method(),
				new Object[]{1, 2});
		assertEquals(method(), invocation.getMethod());
	}

	@Test
	public void testGetResult() throws Exception {
		Invocation invocation = Invocation.root(method(),
				new Object[]{1, 2});
		assertEquals(method().getResult(), invocation.getResult());
	}

	@Test
	public void testGetExc() throws Exception {
		Invocation invocation = Invocation.root(method(),
				new Object[]{1, 2});
		assertEquals(method().getExc(), invocation.getExc());
	}

	@Test
	public void testToChain() throws Exception {
		List<Invocation> chain = Invocation
				.root(interfaceMethod(), new Object[]{1, 2})
				.next(stringMethod(), new Object[]{"hello"})
				.toChain();

		assertEquals(2, chain.size());
	}

	@Test
	public void testInvoke() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		when(iface.method(1, 2)).thenReturn(3);

		Invocation invocation = Invocation.root(method(), new Object[]{1, 2});
		Object result = invocation.invoke(iface);
		assertEquals(3, result);
	}

	@Test
	public void testInvoke_chained() throws Exception {
		TestInterface iface = mock(TestInterface.class, RETURNS_DEEP_STUBS);
		when(iface.interface0(1, 2).string0("world")).thenReturn("goodbye");

		Invocation invocation = Invocation
				.root(interfaceMethod(), new Object[]{1, 2})
				.next(stringMethod(), new Object[]{"world"});

		Object result = invocation.invoke(iface);
		assertEquals("goodbye", result);
	}

	@Test(expected = TestException.class)
	public void testInvoke_exc() throws Exception {
		TestInterface iface = mock(TestInterface.class);
		doThrow(new TestException()).when(iface).exc0();
		Invocation invocation = Invocation.root(excMethod(), new Object[]{});

		invocation.invoke(iface);
	}

	private MethodDescriptor<?, ?> method() {
		return TestInterface.DESCRIPTOR.getMethod("method");
	}

	private MethodDescriptor<?, ?> messageMethod() {
		return TestInterface.DESCRIPTOR.getMethod("message0");
	}

	private MethodDescriptor<?, ?> interfaceMethod() {
		return TestInterface.DESCRIPTOR.getMethod("interface0");
	}

	private MethodDescriptor<?, ?> stringMethod() {
		return TestInterface.DESCRIPTOR.getMethod("string0");
	}

	private MethodDescriptor<?, ?> excMethod() {
		return TestInterface.DESCRIPTOR.getMethod("exc0");
	}
}
