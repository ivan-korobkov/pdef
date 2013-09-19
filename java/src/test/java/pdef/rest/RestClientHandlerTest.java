package pdef.rest;

import com.google.common.base.Function;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Atomics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import pdef.Clients;
import pdef.invocation.Invocation;
import pdef.invocation.InvocationResult;
import pdef.descriptors.ArgDescriptor;
import pdef.descriptors.DataDescriptor;
import pdef.descriptors.Descriptors;
import pdef.descriptors.MessageDescriptor;
import pdef.rpc.*;
import pdef.test.interfaces.TestException;
import pdef.test.interfaces.TestInterface;
import pdef.test.messages.SimpleForm;
import pdef.test.messages.SimpleMessage;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
				.setContent(RpcResult.builder()
						.setStatus(RpcStatus.OK)
						.setData(3)
						.build()
						.toJson());

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
		ArgDescriptor argd = ArgDescriptor.builder()
				.setName("arg")
				.setType(Suppliers.<DataDescriptor>ofInstance(Descriptors.string))
				.build();

		String value = handler.serializePositionalArg(argd, "Привет");
		assert value.equals("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82");
	}

	@Test
	public void testSerializeQueryArg() throws Exception {
		ArgDescriptor argd = ArgDescriptor.builder()
				.setName("arg")
				.setType(Suppliers.<DataDescriptor>ofInstance(Descriptors.int32))
				.build();

		Map<String, String> dst = Maps.newHashMap();
		handler.serializeQueryArg(argd, 123, dst);
		assert dst.equals(ImmutableMap.of("arg", "123"));
	}

	@Test
	public void testSerializeQueryArg_form() throws Exception {
		ArgDescriptor argd = ArgDescriptor.builder()
				.setName("arg")
				.setType(SimpleForm.descriptor())
				.build();

		Map<String, String> dst = Maps.newHashMap();
		SimpleForm msg = SimpleForm.builder()
				.setText("Привет, как дела?")
				.setNumbers(ImmutableList.of(1, 2, 3))
				.setFlag(false)
				.build();
		handler.serializeQueryArg(argd, msg, dst);

		assert dst.equals(ImmutableMap.of(
				"text", "Привет, как дела?",
				"numbers", "[1,2,3]",
				"flag", "false"));
	}

	@Test
	public void testSerializeArgToString_primitive() throws Exception {
		String result = handler.serializeArgToString(Descriptors.int32, 123);

		assert result.equals("123");
	}

	@Test
	public void testSerializeArgToString_primitiveNullToEmptyString() throws Exception {
		String result = handler.serializeArgToString(Descriptors.int32, null);

		assert result.equals("");
	}

	@Test
	public void testSerializeArgToString_string() throws Exception {
		String result = handler.serializeArgToString(Descriptors.string, "привет+ромашки");

		assert result.equals("привет+ромашки");
	}

	@Test
	public void testSerializeArgToString_message() throws Exception {
		MessageDescriptor descriptor = SimpleMessage.descriptor();
		SimpleMessage msg = SimpleMessage.builder()
				.setABool(true)
				.setAnInt16((short) 256)
				.setAString("hello")
				.build();

		String result = handler.serializeArgToString(descriptor, msg);
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
		SimpleMessage msg = SimpleMessage.builder()
				.setAString("hello")
				.setABool(true)
				.setAnInt16((short) 1)
				.build();
		String content = RpcResult.builder()
				.setStatus(RpcStatus.OK)
				.setData(msg.toMap())
				.build()
				.toJson();
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
		TestException exc = TestException.builder()
				.setText("Application exception")
				.build();
		String content = RpcResult.builder()
				.setStatus(RpcStatus.EXCEPTION)
				.setData(exc.toMap())
				.build()
				.toJson();
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
		RpcError expected = ClientError.builder()
				.setText("Bad request")
				.build();

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotFound() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_NOT_FOUND)
				.setContent("Method not found");

		RpcError error = handler.parseError(response);
		RpcError expected = MethodNotFoundError.builder()
				.setText("Method not found")
				.build();

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotAllowed() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_METHOD)
				.setContent("Method not allowed");

		RpcError error = handler.parseError(response);
		RpcError expected = MethodNotAllowedError.builder()
				.setText("Method not allowed")
				.build();

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serviceUnavailable() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_UNAVAILABLE)
				.setContent("Service unavailable");

		RpcError error = handler.parseError(response);
		RpcError expected = ServiceUnavailableError.builder()
				.setText("Service unavailable")
				.build();

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serverError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(12345)
				.setContent("Strange error");

		RpcError error = handler.parseError(response);
		RpcError expected = ServerError.builder()
				.setText("Server error, status=12345, text=Strange error")
				.build();

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
