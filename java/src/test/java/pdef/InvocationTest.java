package pdef;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import pdef.descriptors.Descriptor;
import pdef.descriptors.MethodDescriptor;
import pdef.test.TestException;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InvocationTest {
	@Test
	public void testNext() throws Exception {
		MethodDescriptor method = mock(MethodDescriptor.class);
		Invocation root = Invocation.root();
		Invocation invocation = root.next(method, "arg");

		assertEquals(method, invocation.getMethod());
		assertEquals(root, invocation.getParent());
		assertArrayEquals(new Object[]{"arg"}, invocation.getArgs());
	}

	@Test
	public void testToList() throws Exception {
		MethodDescriptor method0 = mock(MethodDescriptor.class);
		MethodDescriptor method1 = mock(MethodDescriptor.class);

		Invocation invocation = Invocation.root();
		Invocation invocation1 = invocation.next(method0);
		Invocation invocation2 = invocation1.next(method1);

		List<Invocation> list = invocation2.toList();
		assertEquals(ImmutableList.of(invocation1, invocation2), list);
	}

	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor method = mock(MethodDescriptor.class);
		Invocation invocation1 = Invocation.root().next(method, "key", "value");

		Object service = new Object();
		invocation1.invoke(service);
		verify(method).invoke(service, "key", "value");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInvoke_exception() throws Exception {
		Object service = new Object();
		TestException exc = TestException.builder().setText("hello, world").build();

		MethodDescriptor method = mock(MethodDescriptor.class);
		when(method.invoke(service)).thenThrow(exc);
		when(method.getExc()).thenReturn((Descriptor) TestException.DESCRIPTOR);

		Invocation invocation = Invocation.root().next(method);
		InvocationResult result = invocation.invokeChainOn(service);
		assertFalse(result.isSuccess());
		assertTrue(result.isExc());
		assertEquals(exc, result.getResult());
		assertEquals(TestException.DESCRIPTOR, result.getResultDescriptor());
	}

	@Test(expected = TestException.class)
	public void testInvoke_unhandledException() throws Exception {
		Object service = new Object();
		TestException exc = TestException.builder().setText("hello, world").build();

		MethodDescriptor method = mock(MethodDescriptor.class);
		when(method.invoke(service)).thenThrow(exc);

		Invocation invocation = Invocation.root().next(method);
		invocation.invoke(service);
	}

	@Test
	public void testInvoke_chained() throws Exception {
		MethodDescriptor method0 = mock(MethodDescriptor.class);
		MethodDescriptor method1 = mock(MethodDescriptor.class);

		Invocation invocation = Invocation.root()
				.next(method0, 1, 2, 3)
				.next(method1, "hello", "world");

		Object service = new Object();
		Object result0 = new Object();
		Object result1 = "Hello, World";
		when(method0.invoke(service, 1, 2, 3)).thenReturn(result0);
		when(method1.invoke(result0, "hello", "world")).thenReturn(result1);

		InvocationResult result = invocation.invokeChainOn(service);
		assertTrue(result.isSuccess());
		assertEquals(result1, result.getResult());
	}

	@Test
	public void testSerialize() throws Exception {
		MethodDescriptor method = mock(MethodDescriptor.class);
		Invocation invocation = Invocation.root().next(method, "key", "value");
		invocation.serialize();

		verify(method).serialize("key", "value");
	}
}
