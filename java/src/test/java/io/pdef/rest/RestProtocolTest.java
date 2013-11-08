package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Atomics;
import io.pdef.Invocation;
import io.pdef.InvocationProxy;
import io.pdef.Invoker;
import io.pdef.descriptors.Descriptors;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RestProtocolTest {
	private RestProtocol protocol = new RestProtocol();

	// GetRequest.

	@Test
	public void testGetRequest() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).method(1, 2);

		RestRequest request = protocol.getRequest(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/method/1/2", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testGetRequest_query() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).query(1, 2);

		RestRequest request = protocol.getRequest(ref.get());
		assertEquals("/query", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testGetRequest_post() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).post(1, 2);

		RestRequest request = protocol.getRequest(ref.get());
		assertEquals(RestRequest.POST, request.getMethod());
		assertEquals("/post", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getPost());
	}

	@Test
	public void testGetRequest_chainedMethods() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).interface0(1, 2).method(3, 4);

		RestRequest request = protocol.getRequest(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/interface0/1/2/method/3/4", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testGetRequest_urlencodePathArgs() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).string0("привет");

		RestRequest request = protocol.getRequest(ref.get());
		assertEquals("/string0/%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82", request.getPath());
	}

	// toJson.

	@Test
	public void testToJson_noQuotes() throws Exception {
		String result = protocol.toJson(Descriptors.string, "Привет,\" мир!");
		assertEquals("Привет,\\\" мир!", result);
	}

	// GetInvocation.

	@Test
	public void testGetInvocation() throws Exception {
		RestRequest request = new RestRequest().setPath("/method/1/2/");

		Invocation invocation = protocol.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals("method", invocation.getMethod().getName());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testGetInvocation_queryMethod() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/query")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = protocol.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(queryMethod(), invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testGetInvocation_postMethod() throws Exception {
		RestRequest request = new RestRequest()
				.setMethod(RestRequest.POST)
				.setPath("/post")
				.setPost(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = protocol.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(postMethod(), invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test(expected = RestException.class)
	public void testGetInvocation_postMethod_getNotAllowed() throws Exception {
		RestRequest request = new RestRequest().setPath("/post");

		protocol.getInvocation(request, TestInterface.DESCRIPTOR);
	}

	@Test
	public void testGetInvocation_chainedMethods() throws Exception {
		RestRequest request = new RestRequest().setPath("/interface0/1/2/query/")
				.setQuery(ImmutableMap.of("arg0", "3", "arg1", "4"));

		List<Invocation> chain = protocol.getInvocation(request, TestInterface.DESCRIPTOR).toChain();
		assertEquals(2, chain.size());

		Invocation invocation0 = chain.get(0);
		assertEquals(interfaceMethod(), invocation0.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation0.getArgs());

		Invocation invocation1 = chain.get(1);
		assertEquals(queryMethod(), invocation1.getMethod());
		assertArrayEquals(new Object[]{3, 4}, invocation1.getArgs());
	}

	@Test(expected = RestException.class)
	public void testGetInvocation_lastMethodNotRemote() throws Exception {
		RestRequest request = new RestRequest().setPath("/interface0/1/2");

		protocol.getInvocation(request, TestInterface.DESCRIPTOR);
	}

	@Test
	public void testGetInvocation_urldecodePathArgs() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/string0/%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82");

		Invocation invocation = protocol.getInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(stringMethod(), invocation.getMethod());
		assertArrayEquals(new Object[]{"Привет"}, invocation.getArgs());
	}

	// fromJson

	@Test
	public void testFromJson() throws Exception {
		TestMessage expected = new TestMessage()
				.setString0("Привет")
				.setBool0(true)
				.setShort0((short) 123);
		String json = expected.toJson();
		TestMessage result = protocol.fromJson(TestMessage.DESCRIPTOR, json);
		assertEquals(expected, result);
	}

	@Test
	public void testFromJson_unquotedString() throws Exception {
		String result = protocol.fromJson(Descriptors.string, "Привет");
		assertEquals("Привет", result);
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

	private MethodDescriptor<?, ?> method() {
		return TestInterface.DESCRIPTOR.getMethod("method");
	}

	private MethodDescriptor<?, ?> queryMethod() {
		return TestInterface.DESCRIPTOR.getMethod("query");
	}

	private MethodDescriptor<?, ?> postMethod() {
		return TestInterface.DESCRIPTOR.getMethod("post");
	}

	private MethodDescriptor<?, ?> stringMethod() {
		return TestInterface.DESCRIPTOR.getMethod("string0");
	}

	private MethodDescriptor<?, ?> interfaceMethod() {
		return TestInterface.DESCRIPTOR.getMethod("interface0");
	}
}
