package io.pdef.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Atomics;
import io.pdef.descriptors.ArgumentDescriptor;
import io.pdef.descriptors.Descriptors;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.descriptors.ImmutableArgumentDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.Invoker;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestForm;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RestProtocolTest {
	private RestProtocol format = new RestProtocol();

	// GetRequest.

	@Test
	public void testGetRequest() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testIndex(1, 2);

		RestRequest request = format.getRequest(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testGetRequest_post() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testPost(1, 2);

		RestRequest request = format.getRequest(ref.get());
		assertEquals(RestRequest.POST, request.getMethod());
		assertEquals("/testPost", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getPost());
	}

	@Test
	public void testGetRequest_chainedMethod() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testInterface(1, 2).testRemote(3, 4);

		RestRequest request = format.getRequest(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/testInterface/1/2/testRemote", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "3", "arg1", "4"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	// WriteInvocation.

	@Test
	public void testWriteInvocation_indexMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testIndex(1, 2);

		format.writeInvocation(request, ref.get());
		assertEquals("/", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testWriteInvocation_postMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testPost(1, 2);

		format.writeInvocation(request, ref.get());
		assertEquals("/testPost", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getPost());
	}

	@Test
	public void testWriteInvocation_remoteMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testRemote(10, 100);

		format.writeInvocation(request, ref.get());
		assertEquals("/testRemote", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "10", "arg1", "100"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testWriteInvocation_interfaceMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testInterface(1, 2).testIndex(3, 4);

		// Get the first invocation in the chain.
		Invocation interfaceInvocation = ref.get().toChain().get(0);
		format.writeInvocation(request, interfaceInvocation);
		assertEquals("/testInterface/1/2", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertTrue(request.getPost().isEmpty());
	}

	// WriteParam.

	@Test
	public void testWritePathArgument() throws Exception {
		ArgumentDescriptor<String> argd = ImmutableArgumentDescriptor
				.of("arg", Descriptors.string);

		String value = format.writePathArgument(argd, "Привет");
		assertEquals("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82", value);
	}

	@Test
	public void testWriteParam() throws Exception {
		ArgumentDescriptor<Integer> argd = ImmutableArgumentDescriptor
				.of("arg", Descriptors.int32);

		Map<String, String> dst = Maps.newHashMap();
		format.writeParam(argd, 123, dst);
		assertEquals(ImmutableMap.of("arg", "123"), dst);
	}

	@Test
	public void testWriteParam_form() throws Exception {
		ArgumentDescriptor<TestForm> argd = ImmutableArgumentDescriptor
				.of("arg", TestForm.DESCRIPTOR);

		Map<String, String> dst = Maps.newHashMap();
		TestForm msg = new TestForm()
				.setFormString("Привет, как дела?")
				.setFormList(ImmutableList.of(1, 2, 3))
				.setFormBool(false);
		format.writeParam(argd, msg, dst);

		assertEquals(ImmutableMap.of(
				"formString", "Привет, как дела?",
				"formList", "[1,2,3]",
				"formBool", "false"), dst);
	}

	// Invocation parsing.

	@Test
	public void testGetInvocation_indexMethod() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals("testIndex", invocation.getMethod().getName());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testGetInvocation_postMethod() throws Exception {
		RestRequest request = new RestRequest()
				.setMethod(RestRequest.POST)
				.setPath("/testPost")
				.setPost(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(postMethod(), invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test(expected = RestException.class)
	public void testGetInvocation_postMethodNotAllowed() throws Exception {
		RestRequest request = new RestRequest().setPath("/testPost");

		format.getInvocation(request, TestInterface.DESCRIPTOR);
	}

	@Test
	public void testGetInvocation_remoteMethod() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/testRemote")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(remoteMethod(), invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testGetInvocation_chainedMethodIndex() throws Exception {
		RestRequest request = new RestRequest().setPath("/testInterface/1/2/")
				.setQuery(ImmutableMap.of("arg0", "3", "arg1", "4"));

		List<Invocation> chain = format.getInvocation(request, TestInterface.DESCRIPTOR)
				.toChain();
		assertEquals(2, chain.size());

		Invocation invocation0 = chain.get(0);
		assertEquals(interfaceMethod(), invocation0.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation0.getArgs());

		Invocation invocation1 = chain.get(1);
		assertEquals(indexMethod(), invocation1.getMethod());
		assertArrayEquals(new Object[]{3, 4}, invocation1.getArgs());
	}

	@Test
	public void testGetInvocation_chainedMethodRemote() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/testInterface/1/2/testString")
				.setQuery(ImmutableMap.of("text", "Привет"));

		List<Invocation> chain = format.getInvocation(request, TestInterface.DESCRIPTOR).toChain();
		assertEquals(2, chain.size());

		Invocation invocation0 = chain.get(0);
		assertEquals(interfaceMethod(), invocation0.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation0.getArgs());

		Invocation invocation1 = chain.get(1);
		assertEquals(stringMethod(), invocation1.getMethod());
		assertArrayEquals(new Object[]{"Привет"}, invocation1.getArgs());
	}

	@Test(expected = RestException.class)
	public void testGetInvocation_interfaceMethodNotRemote() throws Exception {
		RestRequest request = new RestRequest().setPath("/testInterface/1/2");

		format.getInvocation(request, TestInterface.DESCRIPTOR);
	}

	// Argument parsing.

	@Test
	public void testReadPathArgument() throws Exception {
		ArgumentDescriptor<String> argd = ImmutableArgumentDescriptor
				.of("arg", Descriptors.string);
		String part = "%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82";

		String value = format.readPathArgument(argd, part);
		assertEquals("Привет", value);
	}

	@Test
	public void testReadParam() throws Exception {
		ArgumentDescriptor<TestMessage> argd = ImmutableArgumentDescriptor.of("arg",
				TestMessage.DESCRIPTOR);

		TestMessage expected = new TestMessage()
				.setString0("Привет")
				.setBool0(true)
				.setShort0((short) 123);
		Map<String, String> query = ImmutableMap.of("arg", expected.toJson());

		Object result = format.readParam(argd, query);
		assertEquals(expected, result);
	}

	@Test
	public void testReadParam_form() throws Exception {
		ArgumentDescriptor<TestForm> argd = ImmutableArgumentDescriptor
				.of("arg", TestForm.DESCRIPTOR);

		TestForm expected = new TestForm()
				.setFormString("Привет, как дела?")
				.setFormList(ImmutableList.of(1, 2, 3))
				.setFormBool(false);
		Map<String, String> src = ImmutableMap.of(
				"formString", "Привет, как дела?",
				"formList", "[1,2,3]",
				"formBool", "false");
		TestForm result = format.readParam(argd, src);
		assertEquals(expected, result);
	}

	@Test
	public void testReadParam_message() throws Exception {
		TestMessage msg = new TestMessage()
				.setString0("Привет")
				.setBool0(true)
				.setShort0((short) 123);

		String json = msg.toJson();
		TestMessage result = format.fromJson(msg.descriptor(), json);
		assertEquals(msg, result);
	}

	private TestInterface proxy(final AtomicReference<Invocation> ref) {
		return proxy(new Invoker() {
			@Override
			public Object invoke(final Invocation invocation) {
				ref.set(invocation);
				return null;
			}
		});
	}

	private TestInterface proxy(final Invoker handler) {
		return InvocationProxy.create(TestInterface.DESCRIPTOR, handler);
	}

	private MethodDescriptor<?, ?> postMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testPost");
	}

	private MethodDescriptor<?, ?> remoteMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testRemote");
	}

	private MethodDescriptor<?, ?> interfaceMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testInterface");
	}

	private MethodDescriptor<?, ?> indexMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testIndex");
	}

	private MethodDescriptor<?, ?> stringMethod() {
		return TestInterface.DESCRIPTOR.getMethod("testString");
	}
}
