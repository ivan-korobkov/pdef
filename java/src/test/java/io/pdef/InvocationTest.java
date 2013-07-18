package io.pdef;

import com.google.common.collect.ImmutableList;
import io.pdef.descriptors.MethodDescriptor;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.util.List;

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

		Invocation invocation = Invocation.root();
		Invocation invocation1 = invocation.next(method, "key", "value");

		Object object = new Object();
		invocation1.invoke(object);
		verify(method).invoke(object, "key", "value");
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
