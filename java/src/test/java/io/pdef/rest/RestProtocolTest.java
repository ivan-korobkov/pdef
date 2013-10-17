package io.pdef.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Atomics;
import io.pdef.descriptors.ArgumentDescriptor;
import io.pdef.descriptors.Descriptors;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.InvocationResult;
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

	// Invocation serialization.

	@Test
	public void testSerializeInvocation() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testIndex(1, 2);

		RestRequest request = format.serializeInvocation(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testSerializeInvocation_post() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testPost(1, 2);

		RestRequest request = format.serializeInvocation(ref.get());
		assertEquals(RestRequest.POST, request.getMethod());
		assertEquals("/testPost", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getPost());
	}

	@Test
	public void testSerializeInvocation_chainedMethod() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testInterface(1, 2).testRemote(3, 4);

		RestRequest request = format.serializeInvocation(ref.get());
		assertEquals(RestRequest.GET, request.getMethod());
		assertEquals("/testInterface/1/2/testRemote", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "3", "arg1", "4"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	// Single invocation serialization.

	@Test
	public void testSerializeSingleInvocation_indexMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testIndex(1, 2);

		format.serializeSingleInvocation(request, ref.get());
		assertEquals("/", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testSerializeSingleInvocation_postMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testPost(1, 2);

		format.serializeSingleInvocation(request, ref.get());
		assertEquals("/testPost", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertEquals(ImmutableMap.of("arg0", "1", "arg1", "2"), request.getPost());
	}

	@Test
	public void testSerializeSingleInvocation_remoteMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testRemote(10, 100);

		format.serializeSingleInvocation(request, ref.get());
		assertEquals("/testRemote", request.getPath());
		assertEquals(ImmutableMap.of("arg0", "10", "arg1", "100"), request.getQuery());
		assertTrue(request.getPost().isEmpty());
	}

	@Test
	public void testSerializeSingleInvocation_interfaceMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).testInterface(1, 2).testIndex(3, 4);

		// Get the first invocation in the chain.
		Invocation interfaceInvocation = ref.get().toChain().get(0);
		format.serializeSingleInvocation(request, interfaceInvocation);
		assertEquals("/testInterface/1/2", request.getPath());
		assertTrue(request.getQuery().isEmpty());
		assertTrue(request.getPost().isEmpty());
	}

	// Arguments serialization.

	@Test
	public void testSerializePathArgument() throws Exception {
		ArgumentDescriptor<String> argd = ArgumentDescriptor
				.of("arg", Descriptors.string);

		String value = format.serializePathArgument(argd, "Привет");
		assertEquals("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82", value);
	}

	@Test
	public void testSerializeParam() throws Exception {
		ArgumentDescriptor<Integer> argd = ArgumentDescriptor
				.of("arg", Descriptors.int32);

		Map<String, String> dst = Maps.newHashMap();
		format.serializeParam(argd, 123, dst);
		assertEquals(ImmutableMap.of("arg", "123"), dst);
	}

	@Test
	public void testSerializeParam_form() throws Exception {
		ArgumentDescriptor<TestForm> argd = ArgumentDescriptor.of("arg", TestForm.DESCRIPTOR);

		Map<String, String> dst = Maps.newHashMap();
		TestForm msg = new TestForm()
				.setFormString("Привет, как дела?")
				.setFormList(ImmutableList.of(1, 2, 3))
				.setFormBool(false);
		format.serializeParam(argd, msg, dst);

		assertEquals(ImmutableMap.of(
				"formString", "Привет, как дела?",
				"formList", "[1,2,3]",
				"formBool", "false"), dst);
	}

	@Test
	public void testSerializeToString_primitive() throws Exception {
		String result = format.serializeToJson(Descriptors.int32, 123);

		assertEquals("123", result);
	}

	@Test
	public void testSerializeToString_null() throws Exception {
		String result = format.serializeToJson(Descriptors.int32, null);

		assertEquals("null", result);
	}

	@Test
	public void testSerializeToString_string() throws Exception {
		String result = format.serializeToJson(Descriptors.string, "привет+ромашки");

		assertEquals("привет+ромашки", result);
	}

	@Test
	public void testSerializeToString_message() throws Exception {
		MessageDescriptor type = TestMessage.DESCRIPTOR;
		TestMessage msg = new TestMessage()
				.setBool0(true)
				.setShort0((short) 256)
				.setString0("hello");

		String result = format.serializeToJson(type, msg);
		assertEquals("{\"string0\":\"hello\",\"bool0\":true,\"short0\":256}", result);
	}

	// InvocationResult parsing.

	@Test
	public void testParseInvocationResult_data() throws Exception {
		TestMessage msg = new TestMessage()
				.setString0("hello")
				.setBool0(true)
				.setShort0((short) 1);

		String content = fixtureRestResult(msg).serializeToJson();
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(content);

		InvocationResult result = format.parseInvocationResult(response,
				TestMessage.DESCRIPTOR, null);
		assertTrue(result.isOk());
		assertEquals(msg, result.getData());
	}

	@Test
	public void testParseInvocationResult_exc() throws Exception {
		TestException exc = new TestException().setText("Application exception");

		String content = fixtureExcRestResult(exc).serializeToJson();
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(content);

		InvocationResult result = format.parseInvocationResult(response,
				Descriptors.string, TestException.DESCRIPTOR);
		assertFalse(result.isOk());
		assertEquals(exc, result.getExc());
	}

	// Invocation parsing.

	@Test
	public void testParseInvocation_indexMethod() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.parseInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals("testIndex", invocation.getMethod().getName());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testParseInvocation_postMethod() throws Exception {
		RestRequest request = RestRequest.post()
				.setPath("/testPost")
				.setPost(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.parseInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(TestInterface.TESTPOST_METHOD, invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test(expected = RestException.class)
	public void testParseInvocation_postMethodNotAllowed() throws Exception {
		RestRequest request = RestRequest.get().setPath("/testPost");

		format.parseInvocation(request, TestInterface.DESCRIPTOR);
	}

	@Test
	public void testParseInvocation_remoteMethod() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/testRemote")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));

		Invocation invocation = format.parseInvocation(request, TestInterface.DESCRIPTOR);
		assertEquals(TestInterface.TESTREMOTE_METHOD, invocation.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation.getArgs());
	}

	@Test
	public void testParseInvocation_chainedMethodIndex() throws Exception {
		RestRequest request = new RestRequest().setPath("/testInterface/1/2/")
				.setQuery(ImmutableMap.of("arg0", "3", "arg1", "4"));

		List<Invocation> chain = format.parseInvocation(request, TestInterface.DESCRIPTOR)
				.toChain();
		assertEquals(2, chain.size());

		Invocation invocation0 = chain.get(0);
		assertEquals(TestInterface.TESTINTERFACE_METHOD, invocation0.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation0.getArgs());

		Invocation invocation1 = chain.get(1);
		assertEquals(TestInterface.TESTINDEX_METHOD, invocation1.getMethod());
		assertArrayEquals(new Object[]{3, 4}, invocation1.getArgs());
	}

	@Test
	public void testParseInvocation_chainedMethodRemote() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/testInterface/1/2/testString")
				.setQuery(ImmutableMap.of("text", "Привет"));

		List<Invocation> chain = format.parseInvocation(request, TestInterface.DESCRIPTOR).toChain();
		assertEquals(2, chain.size());

		Invocation invocation0 = chain.get(0);
		assertEquals(TestInterface.TESTINTERFACE_METHOD, invocation0.getMethod());
		assertArrayEquals(new Object[]{1, 2}, invocation0.getArgs());

		Invocation invocation1 = chain.get(1);
		assertEquals(TestInterface.TESTSTRING_METHOD, invocation1.getMethod());
		assertArrayEquals(new Object[]{"Привет"}, invocation1.getArgs());
	}

	@Test(expected = RestException.class)
	public void testParseInvocation_interfaceMethodNotRemote() throws Exception {
		RestRequest request = new RestRequest().setPath("/testInterface/1/2");

		format.parseInvocation(request, TestInterface.DESCRIPTOR);
	}

	// Argument parsing.

	@Test
	public void testParsePathArgument() throws Exception {
		ArgumentDescriptor<String> argd = ArgumentDescriptor
				.of("arg", Descriptors.string);
		String part = "%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82";

		String value = format.parsePathArgument(argd, part);
		assertEquals("Привет", value);
	}

	@Test
	public void testParseParam() throws Exception {
		ArgumentDescriptor<TestMessage> argd = ArgumentDescriptor.of("arg",
				TestMessage.DESCRIPTOR);

		TestMessage expected = new TestMessage()
				.setString0("Привет")
				.setBool0(true)
				.setShort0((short) 123);
		Map<String, String> query = ImmutableMap.of("arg", expected.serializeToJson());

		Object result = format.parseParam(argd, query);
		assertEquals(expected, result);
	}

	@Test
	public void testParseQueryArg_form() throws Exception {
		ArgumentDescriptor<TestForm> argd = ArgumentDescriptor
				.of("arg", TestForm.DESCRIPTOR);

		TestForm expected = new TestForm()
				.setFormString("Привет, как дела?")
				.setFormList(ImmutableList.of(1, 2, 3))
				.setFormBool(false);
		Map<String, String> src = ImmutableMap.of(
				"formString", "Привет, как дела?",
				"formList", "[1,2,3]",
				"formBool", "false");
		TestForm result = format.parseParam(argd, src);
		assertEquals(expected, result);
	}

	@Test
	public void testParseFromString_primitive() throws Exception {
		Integer value = format.parseFromJson(Descriptors.int32, "123");
		assertEquals(123, (int) value);
	}

	@Test
	public void testParseFromString_primitiveEmptyStringToNull() throws Exception {
		Integer value = format.parseFromJson(Descriptors.int32, "");
		assertNull(value);
	}

	@Test
	public void testParseFromString_string() throws Exception {
		String value = format.parseFromJson(Descriptors.string, "Привет");
		assertEquals("Привет", value);
	}

	@Test
	public void testParseFromString_message() throws Exception {
		TestMessage msg = new TestMessage()
				.setString0("Привет")
				.setBool0(true)
				.setShort0((short) 123);

		String json = msg.serializeToJson();
		TestMessage result = format.parseFromJson(msg.descriptor(), json);
		assertEquals(msg, result);
	}

	// Invocation result serialization.

	@Test
	public void testSerializeInvocationResult() throws Exception {
		TestMessage msg = new TestMessage()
				.setString0("hello")
				.setBool0(true)
				.setShort0((short) 123);

		InvocationResult result = InvocationResult.ok(msg);
		String content = fixtureRestResult(msg).serializeToJson();

		RestResponse response = format.serializeInvocationResult(result,
				TestMessage.DESCRIPTOR, null);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test
	public void testSerializeInvocationResult_exc() throws Exception {
		TestException exc = new TestException().setText("hello, world");
		InvocationResult result = InvocationResult.exc(exc);
		String content = fixtureExcRestResult(exc).serializeToJson();

		RestResponse response = format.serializeInvocationResult(result,
				Descriptors.string, TestException.DESCRIPTOR);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	private RestResult<TestMessage, Object> fixtureRestResult(final TestMessage msg) {
		return RestProtocol.resultDescriptor(TestMessage.DESCRIPTOR, null)
				.newInstance()
				.setSuccess(true)
				.setData(msg);
	}

	private RestResult<String, TestException> fixtureExcRestResult(final TestException exc) {
		return RestProtocol.resultDescriptor(Descriptors.string, TestException.DESCRIPTOR)
				.newInstance()
				.setSuccess(false)
				.setExc(exc);
	}

	private TestInterface proxy(final AtomicReference<Invocation> ref) {
		return proxy(new Invoker() {
			@Override
			public InvocationResult invoke(final Invocation invocation) {
				ref.set(invocation);
				return InvocationResult.ok(null);
			}
		});
	}

	private TestInterface proxy(final Invoker handler) {
		return InvocationProxy.create(TestInterface.class, handler);
	}
}
