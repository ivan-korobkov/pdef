package io.pdef;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

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
		assertEquals(ImmutableMap.<String, Object>of("arg", "hello"), invocation.getArgs());
	}

	/** Should ignore null values in args. */
	@Test
	public void testCapture_nulls() throws Exception {
		InterfaceDescriptor<?> iface = new GeneratedInterfaceDescriptor<TestInterface>(
				TestInterface.class);
		MethodDescriptor method = GeneratedMethodDescriptor.builder(iface, "method")
				.arg("arg", Descriptors.string)
				.result(Descriptors.string)
				.build();

		Invocation root = Invocation.root();
		Invocation invocation = method.capture(root, new Object[]{null});

		assertEquals(method, invocation.getMethod());
		assertEquals(root, invocation.getParent());
		assertEquals(ImmutableMap.<String, Object>of(), invocation.getArgs());
	}

	@Test
	public void testParse() throws Exception {
		MethodDescriptor method = createSumMethod();

		Map<String, Object> args = ImmutableMap.<String, Object>of("i0", 123, "unused", 345);
		Invocation invocation = method.parse(Invocation.root(), args);

		assertEquals(method, invocation.getMethod());
		assertEquals(ImmutableMap.<String, Object>of("i0", 123, "i1", 0), invocation.getArgs());
	}

	/** Should replace all non-present args with the defaults and invoke the method. */
	@Test
	public void testInvoke() throws Exception {
		MethodDescriptor method = createSumMethod();

		TestInterface test = mock(TestInterface.class);
		when(test.sum(123, 0)).thenReturn(123);

		Integer result = (Integer) method.invoke(test, ImmutableMap.<String, Object>of("i0", 123));
		assertEquals(123, (int) result);
	}

	/** Should return a new method call with serialized non-null args. */
	@Test
	public void testSerialize() throws Exception {
		MethodDescriptor method = createSumMethod();

		MethodCall call = method.serialize(ImmutableMap.<String, Object>of("i0", 123));
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
