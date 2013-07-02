package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

public class InvocationTest {
	@Test
	public void testNext() throws Exception {
		Invocation invocation = Invocation.root();
		MethodDescriptor method = mock(MethodDescriptor.class);
		Invocation next = invocation.next(method, "value");
		assertEquals(method, next.getMethod());
		assertEquals(invocation, next.getParent());
		assertArrayEquals(new Object[]{"value"}, next.getArgs());
	}

	@Test
	public void testToList() throws Exception {
		Invocation invocation = Invocation.root();
		MethodDescriptor method0 = mock(MethodDescriptor.class);
		MethodDescriptor method1 = mock(MethodDescriptor.class);
		Invocation invocation1 = invocation.next(method0, ImmutableMap.<String, Object>of());
		Invocation invocation2 = invocation1.next(method1, ImmutableMap.<String, Object>of());

		List<Invocation> list = invocation2.toList();
		assertEquals(ImmutableList.of(invocation1, invocation2), list);
	}

	@Test
	public void testInvoke() throws Exception {
		Invocation invocation = Invocation.root();
		MethodDescriptor method = mock(MethodDescriptor.class);

		Map<String, Object> args = ImmutableMap.<String, Object>of("key", "value");
		Invocation invocation1 = invocation.next(method, args);

		Object object = new Object();
		invocation1.invoke(object);
		verify(method).invoke(object, args);
	}

	@Test
	public void testSerialize() throws Exception {
		Invocation invocation = Invocation.root();
		MethodDescriptor method = mock(MethodDescriptor.class);

		Map<String, Object> args = ImmutableMap.<String, Object>of("key", "value");
		Invocation invocation1 = invocation.next(method, args);
		invocation1.serialize();

		verify(method).serialize(args);
	}
}
