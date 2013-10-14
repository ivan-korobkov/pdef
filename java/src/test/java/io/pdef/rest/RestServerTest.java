package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;

public class RestServerTest {
	RestServer<TestInterface> handler;
	@Mock Function<Invocation, InvocationResult> invoker;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		handler = new RestServer<TestInterface>(TestInterface.class, invoker);
	}

	@Test
	public void testHandle() throws Exception {
		handler = RestServer.create(TestInterface.class,
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
		handler = RestServer.create(TestInterface.class,
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

	@Test(expected = IllegalArgumentException.class)
	public void testHandle_restError() throws Exception {
		handler = RestServer.create(TestInterface.class,
				new Function<Invocation, InvocationResult>() {
					@Override
					public InvocationResult apply(final Invocation input) {
						throw new IllegalArgumentException();
					}
				});

		RestRequest request = new RestRequest().setPath("/");
		handler.apply(request);
	}

	// errorResponse.

	@Test
	public void testErrorResponse() throws Exception {
		RestException exc = RestException.serviceUnavailable("Test service unavailable");
		RestResponse response = handler.errorResponse(exc);

		assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, response.getStatus());
		assertEquals(RestResponse.TEXT_CONTENT_TYPE, response.getContentType());
		assertEquals(exc.getMessage(), response.getContent());
	}
}
