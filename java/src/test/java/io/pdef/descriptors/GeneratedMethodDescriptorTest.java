package io.pdef.descriptors;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import io.pdef.Invocation;
import io.pdef.rpc.RpcCall;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeneratedMethodDescriptorTest {
	@Test
	public void testBuild_remote() throws Exception {
		InterfaceDescriptor<?> iface = new GeneratedInterfaceDescriptor<TestInterface>(
				TestInterface.class);

		MethodDescriptor method = GeneratedMethodDescriptor.builder(iface, "method")
				.arg("arg", Descriptors.string)
				.result(Descriptors.string)
				.build();

		assertEquals(ImmutableMap.<String, Descriptor<?>>of("arg", Descriptors.string),
				method.getArgs());
		assertEquals(Descriptors.string, method.getResult());
		assertTrue(method.isRemote());
	}

	@Test
	public void testBuild_notRemote() throws Exception {
		final InterfaceDescriptor<?> iface = new GeneratedInterfaceDescriptor<TestInterface>(
				TestInterface.class);
		MethodDescriptor method = GeneratedMethodDescriptor.builder(iface, "method")
				.arg("arg", Descriptors.string)
				.next(new Supplier<InterfaceDescriptor<?>>() {
					@Override
					public InterfaceDescriptor<?> get() {
						return iface;
					}
				})
				.build();

		assertFalse(method.isRemote());
		assertEquals(iface, method.getNext());
	}

	@Test
	public void testCapture() throws Exception {
		InterfaceDescriptor<?> iface = new GeneratedInterfaceDescriptor<TestInterface>(
				TestInterface.class);
		MethodDescriptor method = GeneratedMethodDescriptor.builder(iface, "method")
				.arg("arg", Descriptors.string)
				.result(Descriptors.string)
				.build();

		Invocation root = Invocation.root();
		Invocation invocation = method.capture(root, "hello");

		assertEquals(method, invocation.getMethod());
		assertEquals(root, invocation.getParent());
		assertArrayEquals(new Object[] {"hello"}, invocation.getArgs());
	}

	@Test
	public void testParse() throws Exception {
		MethodDescriptor method = createSumMethod();

		Map<String, Object> args = ImmutableMap.<String, Object>of("i0", 123, "unused", 345);
		Invocation invocation = method.parse(Invocation.root(), args);

		assertEquals(method, invocation.getMethod());
		assertArrayEquals(new Object[]{123, 0}, invocation.getArgs());
	}

	/** Should replace all non-present args with the defaults and invoke the method. */
	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor method = createSumMethod();

		TestInterface test = mock(TestInterface.class);
		when(test.sum(123, 10)).thenReturn(133);

		Integer result = (Integer) method.invoke(test, 123, 10);
		assertEquals(133, (int) result);
	}

	/** Should return a new method call with serialized non-null args. */
	@Test
	public void testSerialize() throws Exception {
		MethodDescriptor method = createSumMethod();

		RpcCall call = method.serialize(123, null);
		assertEquals("sum", call.getMethod());
		assertEquals(ImmutableMap.<String, Object>of("i0", 123, "i1", 0), call.getArgs());
	}

	private MethodDescriptor createSumMethod() {
		InterfaceDescriptor<?> iface = new GeneratedInterfaceDescriptor<TestInterface>(
				TestInterface.class);
		return GeneratedMethodDescriptor.builder(iface, "sum")
				.arg("i0", Descriptors.int32)
				.arg("i1", Descriptors.int32)
				.result(Descriptors.int32)
				.build();
	}

	public static interface TestInterface {
		String method(String arg);

		int sum(int i0, int i1);
	}
}
