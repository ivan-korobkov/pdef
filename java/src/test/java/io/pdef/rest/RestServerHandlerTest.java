package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;

public class RestServerHandlerTest {
	RestServerHandler<TestInterface> handler;
	@Mock Function<Invocation, InvocationResult> invoker;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		handler = new RestServerHandler<TestInterface>(TestInterface.class, invoker);
	}

	@Test
	public void testHandle() throws Exception {
		handler = RestServerHandler.create(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.ok(3);
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = RestFormat.resultDescriptor(Descriptors.int32, null).newInstance()
				.setSuccess(true)
				.setData(3)
				.serializeToJson(true);

		RestResponse response = handler.apply(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test
	public void testHandle_exc() throws Exception {
		final TestException exc = new TestException()
				.setText("Hello, world");
		handler = RestServerHandler.create(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.exc(exc);
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = RestFormat.resultDescriptor(Descriptors.int32, TestException.DESCRIPTOR)
				.newInstance()
				.setSuccess(false)
				.setExc(exc)
				.serializeToJson();

		RestResponse response = handler.apply(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test
	public void testHandle_error() throws Exception {
		handler = RestServerHandler.create(TestInterface.class,
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
		assertNotNull(response);
		assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatus());
		assertTrue(response.hasTextContentType());
		assertEquals("Internal server error", response.getContent());
	}

	// errorResponse.

	@Test
	public void testErrorResponse_wrongMethodArgs() throws Exception {
		WrongMethodArgsError error = new WrongMethodArgsError()
				.setText("Wrong method args");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_methodNotFound() throws Exception {
		MethodNotFoundError error = new MethodNotFoundError()
				.setText("Method not found");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_methodNotAllowed() throws Exception {
		MethodNotAllowedError error = new MethodNotAllowedError()
				.setText("Method not allowed");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_clientError() throws Exception {
		ClientError error = new ClientError()
				.setText("Bad request");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_serviceUnavailable() throws Exception {
		ServiceUnavailableError error = new ServiceUnavailableError()
				.setText("Service unavailable");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_serverError() throws Exception {
		ServerError error = new ServerError().setText("Internal server error");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(error.getText(), response.getContent());
	}

	@Test
	public void testErrorResponse_unhandledError() throws Exception {
		RuntimeException error = new RuntimeException("Goodbye, world");

		RestResponse response = handler.handleException(error);
		assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, response.getStatus());
		assertEquals(Rest.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals("Internal server error", response.getContent());
	}
}
