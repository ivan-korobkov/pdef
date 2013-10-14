package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.annotation.Nullable;
import java.net.HttpURLConnection;

public class RestServerTest {
	@Test
	public void testHandle() throws Exception {
		RestServer<TestInterface> server = RestServer.builder(TestInterface.class)
				.setInvoker(new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.ok(3);
					}
				})
				.build();

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = RestFormat.resultDescriptor(Descriptors.int32, null).newInstance()
				.setSuccess(true)
				.setData(3)
				.serializeToJson(true);

		RestResponse response = server.apply(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test
	public void testHandle_exc() throws Exception {
		final TestException exc = new TestException().setText("Hello, world");
		RestServer<TestInterface> server = RestServer.builder(TestInterface.class)
				.setInvoker(new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						return InvocationResult.exc(exc);
					}
				})
				.build();

		RestRequest request = new RestRequest()
				.setPath("/remoteMethod")
				.setQuery(ImmutableMap.of("a", "1", "b", "2"));
		String content = RestFormat.resultDescriptor(Descriptors.int32, TestException.DESCRIPTOR)
				.newInstance()
				.setSuccess(false)
				.setExc(exc)
				.serializeToJson();

		RestResponse response = server.apply(request);
		assertNotNull(response);
		assertTrue(response.hasOkStatus());
		assertTrue(response.hasJsonContentType());
		assertEquals(content, response.getContent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHandle_restError() throws Exception {
		RestServer<TestInterface> server = RestServer.builder(TestInterface.class)
				.setInvoker(new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						throw new IllegalArgumentException();
					}
				})
				.build();

		RestRequest request = new RestRequest().setPath("/");
		server.apply(request);
	}

	// errorResponse.

	@Test
	public void testErrorResponse() throws Exception {
		RestException exc = RestException.serviceUnavailable("Test service unavailable");
		RestResponse response = RestServer.builder(TestInterface.class)
				.setInvoker(new Function<Invocation, InvocationResult>() {
					@Nullable
					@Override
					public InvocationResult apply(@Nullable final Invocation input) {
						return null;
					}
				})
				.build()
				.errorResponse(exc);

		assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, response.getStatus());
		assertEquals(RestResponse.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(exc.getMessage(), response.getContent());
	}
}
