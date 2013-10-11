package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;

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

		RestResponse response = handler.apply(request);
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

		RestResponse response = handler.apply(request);
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
		RestResponse response = handler.apply(request);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.hasTextContentType();
		assert response.getContent().equals("Internal server error");
	}

	// errorResponse.

	@Test
	public void testErrorResponse_wrongMethodArgs() throws Exception {
		WrongMethodArgsError error = new WrongMethodArgsError()
				.setText("Wrong method args");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_methodNotFound() throws Exception {
		MethodNotFoundError error = new MethodNotFoundError()
				.setText("Method not found");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_methodNotAllowed() throws Exception {
		MethodNotAllowedError error = new MethodNotAllowedError()
				.setText("Method not allowed");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_METHOD;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_clientError() throws Exception {
		ClientError error = new ClientError()
				.setText("Bad request");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_serviceUnavailable() throws Exception {
		ServiceUnavailableError error = new ServiceUnavailableError()
				.setText("Service unavailable");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_UNAVAILABLE;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_serverError() throws Exception {
		ServerError error = new ServerError().setText("Internal server error");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals(error.getText());
	}

	@Test
	public void testErrorResponse_unhandledError() throws Exception {
		RuntimeException error = new RuntimeException("Goodbye, world");

		RestResponse response = handler.handleException(error);
		assert response.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR;
		assert response.getContentType().equals(Rest.TEXT_CONTENT_TYPE);
		assert response.getContent().equals("Internal server error");
	}
}
