package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;

public class RestServletTest {
	@Mock RestHandler<TestInterface> handler;
	RestServlet<TestInterface> servlet;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		servlet = new RestServlet<TestInterface>(handler);
	}

	@Test
	public void getRestRequest() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn(RestRequest.GET);
		when(request.getServletPath()).thenReturn("/my/app");
		when(request.getPathInfo()).thenReturn("/method1/method2");
		when(request.getParameterMap()).thenReturn(ImmutableMap.of(
				"key0", new String[]{"value0"},
				"key1", new String[]{"value1", "value11"}));

		RestRequest req = servlet.getRestRequest(request);
		assertEquals(RestRequest.GET, req.getMethod());
		assertEquals("/method1/method2", req.getPath());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getQuery());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getPost());
	}

	@Test
	public void testWriteResult_ok() throws Exception {
		RestResult<String> result = RestResult.ok("Привет", Descriptors.string);
		HttpServletResponse response = mockResponse();
		servlet.writeResult(result, response);

		verify(response).setStatus(HttpURLConnection.HTTP_OK);
		verify(response).setContentType(RestServlet.JSON_CONTENT_TYPE);
	}

	@Test
	public void testWriteResult_applicationException() throws Exception {
		TestException e = new TestException().setText("Привет");
		RestResult<TestException> result = RestResult.exc(e, TestException.DESCRIPTOR);
		HttpServletResponse response = mockResponse();
		servlet.writeResult(result, response);

		verify(response).setStatus(RestServlet.APPLICATION_EXC_STATUS);
		verify(response).setContentType(RestServlet.JSON_CONTENT_TYPE);
	}

	@Test
	public void testWriteRestException() throws Exception {
		RestException exception = RestException.methodNotFound("Method not found");
		HttpServletResponse response = mockResponse();
		servlet.writeRestException(exception, response);

		verify(response).setStatus(HttpURLConnection.HTTP_NOT_FOUND);
		verify(response).setContentType(RestServlet.TEXT_CONTENT_TYPE);
	}

	private HttpServletResponse mockResponse() {
		return mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
	}
}
