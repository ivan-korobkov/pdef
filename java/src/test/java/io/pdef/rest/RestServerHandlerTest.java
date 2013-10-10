package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.SimpleForm;
import io.pdef.test.messages.SimpleMessage;
import io.pdef.meta.InterfaceMethodArg;
import io.pdef.meta.MetaTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RestServerHandlerTest {
	RestServerHandler handler;
	@Mock Function<Invocation, InvocationResult> invoker;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		handler = new RestServerHandler(TestInterface.class, invoker);
	}

	@Test
	public void testHandle() throws Exception {
		handler = new RestServerHandler(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.ok(3);
					}
				});
		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = new RpcResult()
				.setStatus(RpcStatus.OK)
				.setData(3)
				.serializeToJson();

		RestResponse response = handler.handle(request);
		assert response.hasOkStatus();
		assert response.hasJsonContentType();
		assert response.getContent().equals(content);
	}

	@Test
	public void testHandle_exc() throws Exception {
		final TestException exc = new TestException()
				.setText("Hello, world");
		handler = new RestServerHandler(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.exc(exc);
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = new RpcResult()
				.setStatus(RpcStatus.EXCEPTION)
				.setData(exc.serializeToMap())
				.serializeToJson();

		RestResponse response = handler.handle(request);
		assert response.hasOkStatus();
		assert response.hasJsonContentType();
		assert response.getContent().equals(content);
	}

	@Test
	public void testHandle_error() throws Exception {
		handler = new RestServerHandler(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						throw new RuntimeException();
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		RestResponse response = handler.handle(request);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.hasTextContentType();
		assert response.getContent().equals("Internal server error");
	}

	// parseRequest.

	@Test
	public void testParseRequest_indexMethod() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));

		Invocation invocation = handler.parseRequest(request);
		assert invocation.getMethod().name().equals("indexMethod");
		assert Arrays.equals(invocation.getArgs(), new Object[]{1, 2});
	}

	@Test
	public void testParseRequest_postMethod() throws Exception {
		RestRequest request = RestRequest.post()
				.setPath("/postMethod")
				.setPost(ImmutableMap.of(
						"aList", "[1, 2, 3]",
						"aMap", "{\"1\": 2}"));

		Invocation invocation = handler.parseRequest(request);
		assert invocation.getMethod().name().equals("postMethod");
		assert Arrays.equals(invocation.getArgs(), new Object[]{
				ImmutableList.of(1, 2, 3),
				ImmutableMap.of(1, 2)
		});
	}

	@Test(expected = MethodNotAllowedError.class)
	public void testParseRequest_postMethodNotAllowed() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/postMethod");

		handler.parseRequest(request);
	}

	@Test
	public void testParseRequest_remoteMethod() throws Exception {
		RestRequest request = RestRequest.get()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));

		Invocation invocation = handler.parseRequest(request);
		assert invocation.getMethod().name().equals("remoteMethod");
		assert Arrays.equals(invocation.getArgs(), new Object[]{1, 2});
	}

	@Test
	public void testParseRequest_chainedMethodIndex() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/interfaceMethod/1/2/");

		List<Invocation> chain = handler.parseRequest(request).toChain();
		assert chain.size() == 2;

		Invocation invocation0 = chain.get(0);
		assert invocation0.getMethod().name().equals("interfaceMethod");
		assert Arrays.equals(invocation0.getArgs(), new Object[]{1, 2});

		Invocation invocation1 = chain.get(1);
		assert invocation1.getMethod().name().equals("indexMethod");
		assert Arrays.equals(invocation1.getArgs(), new Object[0]);
	}

	@Test
	public void testParseRequest_chainedMethodRemote() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/interfaceMethod/1/2/stringMethod")
				.setQuery(ImmutableMap.of("text", "Привет"));

		List<Invocation> chain = handler.parseRequest(request).toChain();
		assert chain.size() == 2;

		Invocation invocation0 = chain.get(0);
		assert invocation0.getMethod().name().equals("interfaceMethod");
		assert Arrays.equals(invocation0.getArgs(), new Object[]{1, 2});

		Invocation invocation1 = chain.get(1);
		assert invocation1.getMethod().name().equals("stringMethod");
		assert Arrays.equals(invocation1.getArgs(), new Object[]{"Привет"});
	}

	@Test(expected = MethodNotFoundError.class)
	public void testParseRequest_interfaceMethodNotRemote() throws Exception {
		RestRequest request = new RestRequest()
				.setPath("/interfaceMethod/1/2");

		handler.parseRequest(request);
	}

	// parseArgs.

	@Test
	public void testParsePositionalArg() throws Exception {
		InterfaceMethodArg<String> argd = InterfaceMethodArg.of("arg", MetaTypes.string);
		String part = "%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82";

		String value = (String) handler.parsePositionalArg(argd, part);
		assert value.equals("Привет");
	}

	@Test
	public void testParseQueryArg() throws Exception {
		InterfaceMethodArg<SimpleMessage> argd = InterfaceMethodArg.of("arg",
				SimpleMessage.META_TYPE);

		SimpleMessage expected = new SimpleMessage()
				.setAString("Привет")
				.setABool(true)
				.setAnInt16((short) 123);
		Map<String, String> query = ImmutableMap.of("arg", expected.serializeToJson());

		Object result = handler.parseQueryArg(argd, query);
		assert result.equals(expected);
	}

	@Test
	public void testParseQueryArg_form() throws Exception {
		InterfaceMethodArg<SimpleForm> argd = InterfaceMethodArg.of("arg", SimpleForm.META_TYPE);

		SimpleForm expected = new SimpleForm()
				.setText("Привет, как дела?")
				.setNumbers(ImmutableList.of(1, 2, 3))
				.setFlag(false);
		Map<String, String> src = ImmutableMap.of(
				"text", "Привет, как дела?",
				"numbers", "[1,2,3]",
				"flag", "false");
		SimpleForm result = (SimpleForm) handler.parseQueryArg(argd, src);
		assert result.equals(expected);
	}

	@Test
	public void testParseArgFromString_primitive() throws Exception {
		Integer value = (Integer) handler.parseArgFromString(MetaTypes.int32, "123");
		assert value == 123;
	}

	@Test
	public void testParseArgFromString_primitiveEmptyStringToNull() throws Exception {
		Integer value = (Integer) handler.parseArgFromString(MetaTypes.int32, "");
		assert value == null;
	}

	@Test
	public void testParseArgFromString_string() throws Exception {
		String value = (String) handler.parseArgFromString(MetaTypes.string, "Привет");
		assert value.equals("Привет");
	}

	@Test
	public void testParseArgFromString_message() throws Exception {
		SimpleMessage msg = new SimpleMessage()
				.setAString("Привет")
				.setABool(true)
				.setAnInt16((short) 123);

		String json = msg.serializeToJson();
		SimpleMessage result = (SimpleMessage) handler.parseArgFromString(msg.metaType(), json);
		assert result.equals(msg);
	}

	// okResponse.

	@Test
	public void testOkResponse() throws Exception {
		RestRequest request = new RestRequest().setPath("/messageMethod");
		Invocation invocation = handler.parseRequest(request);
		SimpleMessage msg = new SimpleMessage()
				.setAString("hello")
				.setABool(true)
				.setAnInt16((short) 123);

		InvocationResult result = InvocationResult.ok(msg);
		String content = new RpcResult()
				.setStatus(RpcStatus.OK)
				.setData(msg.serializeToMap())
				.serializeToJson();

		RestResponse response = handler.okResponse(result, invocation);
		assert response.hasOkStatus();
		assert response.hasJsonContentType();
		assert response.getContent().equals(content);
	}

	@Test
	public void testOkResponse_exception() throws Exception {
		RestRequest request = new RestRequest().setPath("/");
		Invocation invocation = handler.parseRequest(request);
		TestException exc = new TestException()
				.setText("hello, world");

		InvocationResult result = InvocationResult.exc(exc);
		String content = new RpcResult()
				.setStatus(RpcStatus.EXCEPTION)
				.setData(exc.serializeToMap())
				.serializeToJson();

		RestResponse response = handler.okResponse(result, invocation);
		assert response.hasOkStatus();
		assert response.hasJsonContentType();
		assert response.getContent().equals(content);
	}

	// errorResponse.

	@Test
	public void testErrorResponse_wrongMethodArgs() throws Exception {
		WrongMethodArgsError error = new WrongMethodArgsError()
				.setText("Wrong method args");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_methodNotFound() throws Exception {
		MethodNotFoundError error = new MethodNotFoundError()
				.setText("Method not found");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_methodNotAllowed() throws Exception {
		MethodNotAllowedError error = new MethodNotAllowedError()
				.setText("Method not allowed");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_METHOD;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_clientError() throws Exception {
		ClientError error = new ClientError()
				.setText("Bad request");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_serviceUnavailable() throws Exception {
		ServiceUnavailableError error = new ServiceUnavailableError()
				.setText("Service unavailable");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_UNAVAILABLE;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_serverError() throws Exception {
		ServerError error = new ServerError().setText("Internal server error");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_unhandledError() throws Exception {
		RuntimeException error = new RuntimeException("Goodbye, world");

		RestResponse response = handler.errorResponse(error);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals("Internal server error");
	}
}
