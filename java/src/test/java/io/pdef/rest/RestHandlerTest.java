package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.Invoker;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

import java.net.HttpURLConnection;

public class RestHandlerTest {
	@Test
	public void testHandle() throws Exception {
		RestHandler<TestInterface> server = RestHandler.create(TestInterface.class,
				new Invoker() {
					@Override
					public InvocationResult invoke(final Invocation invocation) {
						return InvocationResult.ok(3);
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/testRemote")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));
		String content = RestProtocol.resultDescriptor(Descriptors.int32, null).newInstance()
				.setSuccess(true)
				.setData(3)
				.toJson(true);

		RestResponse response = server.handle(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test
	public void testHandle_exc() throws Exception {
		final TestException exc = new TestException().setText("Hello, world");
		RestHandler<TestInterface> server = RestHandler.create(TestInterface.class,
				new Invoker() {
					@Override
					public InvocationResult invoke(final Invocation invocation) {
						return InvocationResult.exc(exc);
					}
				});

		RestRequest request = new RestRequest()
				.setPath("/testRemote")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));
		String content = RestProtocol.resultDescriptor(Descriptors.int32, TestException.DESCRIPTOR)
				.newInstance()
				.setSuccess(false)
				.setExc(exc)
				.toJson();

		RestResponse response = server.handle(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHandle_restError() throws Exception {
		RestHandler<TestInterface> server = RestHandler.create(TestInterface.class,
				new Invoker() {
					@Override
					public InvocationResult invoke(final Invocation invocation) {
						throw new IllegalArgumentException();
					}
				});

		RestRequest request = new RestRequest().setPath("/");
		server.handle(request);
	}

	// errorResponse.

	@Test
	public void testErrorResponse() throws Exception {
		RestException exc = RestException.serviceUnavailable("Test service unavailable");
		RestResponse response = RestHandler.create(TestInterface.class,
				new Invoker() {
					@Override
					public InvocationResult invoke(final Invocation invocation) {
						return null;
					}
				})
				.errorResponse(exc);

		assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, response.getStatus());
		assertEquals(RestResponse.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(exc.getMessage(), response.getContent());
	}
}
