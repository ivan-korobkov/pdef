package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Atomics;
import io.pdef.Clients;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.SimpleForm;
import io.pdef.test.messages.SimpleMessage;
import io.pdef.meta.InterfaceMethodArg;
import io.pdef.meta.MessageType;
import io.pdef.meta.MetaTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RestClientHandlerTest {
	@Mock Function<RestRequest, RestResponse> sender;
	RestClientHandler handler;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		handler = new RestClientHandler(sender);
	}

	@Test
	public void testInvoke() throws Exception {
		RestRequest request = new RestRequest()
				.setMethod(Rest.GET)
				.setPath("/")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(new RpcResult().setStatus(RpcStatus.OK).setData(3).serializeToJson());

		when(sender.apply(request)).thenReturn(response);
		int result = proxy(handler).indexMethod(1, 2);

		assert result == 3;
	}

	// createRequest.

	@Test
	public void testCreateRequest() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).indexMethod(1, 2);

		RestRequest request = handler.createRequest(ref.get());
		assert request.getMethod().equals(Rest.GET);
		assert request.getPath().equals("/");
		assert request.getQuery().equals(ImmutableMap.of("a", "1", "b", "2"));
		assert request.getPost().isEmpty();
	}

	@Test
	public void testCreateRequest_post() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).postMethod(ImmutableList.of(1, 2, 3), ImmutableMap.of(4, 5));

		RestRequest request = handler.createRequest(ref.get());
		assert request.getMethod().equals(Rest.POST);
		assert request.getPath().equals("/postMethod");
		assert request.getQuery().isEmpty();
		assert request.getPost().equals(ImmutableMap.of(
				"aList", "[1,2,3]",
				"aMap", "{\"4\":5}"
		));
	}

	@Test
	public void testCreateRequest_chainedMethod() throws Exception {
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).interfaceMethod(1, 2).stringMethod("hello");

		RestRequest request = handler.createRequest(ref.get());
		assert request.getMethod().equals(Rest.GET);
		assert request.getPath().equals("/interfaceMethod/1/2/stringMethod");
		assert request.getQuery().equals(ImmutableMap.of("text", "hello"));
		assert request.getPost().isEmpty();
	}

	// serializeInvocation.

	@Test
	public void testSerializeInvocation_indexMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).indexMethod(1, 2);

		handler.serializeInvocation(request, ref.get());
		assert request.getPath().equals("/");
		assert request.getQuery().equals(ImmutableMap.of("a", "1", "b", "2"));
		assert request.getPost().isEmpty();
	}

	@Test
	public void testSerializeInvocation_postMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).postMethod(ImmutableList.of(1, 2, 3), ImmutableMap.of(4, 5));

		handler.serializeInvocation(request, ref.get());
		assert request.getPath().equals("/postMethod");
		assert request.getQuery().isEmpty();
		assert request.getPost().equals(ImmutableMap.of(
				"aList", "[1,2,3]",
				"aMap", "{\"4\":5}"
		));
	}

	@Test
	public void testSerializeInvocation_remoteMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).remoteMethod(10, 100);

		handler.serializeInvocation(request, ref.get());
		assert request.getPath().equals("/remoteMethod");
		assert request.getQuery().equals(ImmutableMap.of("a", "10", "b", "100"));
		assert request.getPost().isEmpty();
	}

	@Test
	public void testSerializeInvocation_interfaceMethod() throws Exception {
		RestRequest request = new RestRequest();
		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).interfaceMethod(1, 2).indexMethod();

		// Get the first invocation in the chain.
		Invocation interfaceInvocation = ref.get().toChain().get(0);
		handler.serializeInvocation(request, interfaceInvocation);
		assert request.getPath().equals("/interfaceMethod/1/2");
		assert request.getQuery().isEmpty();
		assert request.getPost().isEmpty();
	}

	// serializeArgs.

	@Test
	public void testSerializePositionalArg() throws Exception {
		InterfaceMethodArg<String> argd = InterfaceMethodArg.of("arg", MetaTypes.string);

		String value = handler.serializePositionalArg(argd, "Привет");
		assert value.equals("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82");
	}

	@Test
	public void testSerializeQueryArg() throws Exception {
		InterfaceMethodArg<Integer> argd = InterfaceMethodArg.of("arg", MetaTypes.int32);

		Map<String, String> dst = Maps.newHashMap();
		handler.serializeQueryArg(argd, 123, dst);
		assert dst.equals(ImmutableMap.of("arg", "123"));
	}

	@Test
	public void testSerializeQueryArg_form() throws Exception {
		InterfaceMethodArg<SimpleForm> argd = InterfaceMethodArg.of("arg", SimpleForm.META_TYPE);

		Map<String, String> dst = Maps.newHashMap();
		SimpleForm msg = new SimpleForm()
				.setText("Привет, как дела?")
				.setNumbers(ImmutableList.of(1, 2, 3))
				.setFlag(false);
		handler.serializeQueryArg(argd, msg, dst);

		assert dst.equals(ImmutableMap.of(
				"text", "Привет, как дела?",
				"numbers", "[1,2,3]",
				"flag", "false"));
	}

	@Test
	public void testSerializeArgToString_primitive() throws Exception {
		String result = handler.serializeArgToString(MetaTypes.int32, 123);

		assert result.equals("123");
	}

	@Test
	public void testSerializeArgToString_primitiveNullToEmptyString() throws Exception {
		String result = handler.serializeArgToString(MetaTypes.int32, null);

		assert result.equals("");
	}

	@Test
	public void testSerializeArgToString_string() throws Exception {
		String result = handler.serializeArgToString(MetaTypes.string, "привет+ромашки");

		assert result.equals("привет+ромашки");
	}

	@Test
	public void testSerializeArgToString_message() throws Exception {
		MessageType type = SimpleMessage.META_TYPE;
		SimpleMessage msg = new SimpleMessage()
				.setABool(true)
				.setAnInt16((short) 256)
				.setAString("hello");

		String result = handler.serializeArgToString(type, msg);
		assert result.equals("{\"aString\":\"hello\",\"aBool\":true,\"anInt16\":256}");
	}

	// isSuccessful.

	@Test
	public void testIsSuccessful_ok() throws Exception {
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType();

		assert handler.isSuccessful(response);
	}

	@Test
	public void testIsSuccessful_error() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(123)
				.setJsonContentType();

		assert !handler.isSuccessful(response);
	}

	// parseResult.

	@Test
	public void testParseResponse_ok() throws Exception {
		SimpleMessage msg = new SimpleMessage()
				.setAString("hello")
				.setABool(true)
				.setAnInt16((short) 1);
		String content = new RpcResult()
				.setStatus(RpcStatus.OK)
				.setData(msg.serializeToMap())
				.serializeToJson();
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(content);

		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).messageMethod(msg);

		InvocationResult result = handler.parseResult(response, ref.get());
		assert result.isOk();
		assert result.getData().equals(msg);
	}

	@Test
	public void testParseResponse_exc() throws Exception {
		TestException exc = new TestException()
				.setText("Application exception");
		String content = new RpcResult()
				.setStatus(RpcStatus.EXCEPTION)
				.setData(exc.serializeToMap())
				.serializeToJson();
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(content);

		AtomicReference<Invocation> ref = Atomics.newReference();
		proxy(ref).excMethod();

		InvocationResult result = handler.parseResult(response, ref.get());
		assert !result.isOk();
		assert result.getData().equals(exc);
	}

	// parseError.

	@Test
	public void testParseError_clientError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_REQUEST)
				.setContent("Bad request");

		RpcError error = handler.parseError(response);
		RpcError expected = new ClientError()
				.setText("Bad request");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotFound() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_NOT_FOUND)
				.setContent("Method not found");

		RpcError error = handler.parseError(response);
		RpcError expected = new MethodNotFoundError()
				.setText("Method not found");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotAllowed() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_METHOD)
				.setContent("Method not allowed");

		RpcError error = handler.parseError(response);
		RpcError expected = new MethodNotAllowedError()
				.setText("Method not allowed");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serviceUnavailable() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_UNAVAILABLE)
				.setContent("Service unavailable");

		RpcError error = handler.parseError(response);
		RpcError expected = new ServiceUnavailableError()
				.setText("Service unavailable");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serverError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(12345)
				.setContent("Strange error");

		RpcError error = handler.parseError(response);
		RpcError expected = new ServerError()
				.setText("Server error, status=12345, text=Strange error");

		assert error.equals(expected);
	}

	private TestInterface proxy(final AtomicReference<Invocation> ref) {
		Function<Invocation, InvocationResult> handler = new Function<Invocation, InvocationResult>() {
			@Override
			public InvocationResult apply(final Invocation invocation) {
				ref.set(invocation);
				return InvocationResult.ok(null);
			}
		};

		return proxy(handler);
	}

	private TestInterface proxy(final Function<Invocation, InvocationResult> handler) {
		return Clients.client(TestInterface.class, handler);
	}
}
