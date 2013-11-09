package io.pdef.rpc;

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

public class RpcServletTest {
	@Mock RpcHandler<TestInterface> handler;
	RpcServlet<TestInterface> servlet;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		servlet = new RpcServlet<TestInterface>(handler);
	}

	@Test
	public void testGetRpcRequest() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn(RpcRequest.GET);
		when(request.getContextPath()).thenReturn("/my/app");
		when(request.getServletPath()).thenReturn("/method1");
		when(request.getPathInfo()).thenReturn("/method2");
		when(request.getParameterMap()).thenReturn(ImmutableMap.of(
				"key0", new String[]{"value0"},
				"key1", new String[]{"value1", "value11"}));

		RpcRequest req = servlet.getRpcRequest(request);
		assertEquals(RpcRequest.GET, req.getMethod());
		assertEquals("/method1/method2", req.getPath());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getQuery());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getPost());
	}

	@Test
	public void testWriteResult_ok() throws Exception {
		RpcResult<String> result = RpcResult.ok("Привет", Descriptors.string);
		HttpServletResponse response = mockResponse();
		servlet.writeResult(result, response);

		verify(response).setStatus(HttpURLConnection.HTTP_OK);
		verify(response).setContentType(RpcServlet.JSON_CONTENT_TYPE);
	}

	@Test
	public void testWriteResult_applicationException() throws Exception {
		TestException e = new TestException().setText("Привет");
		RpcResult<TestException> result = RpcResult.exc(e, TestException.DESCRIPTOR);
		HttpServletResponse response = mockResponse();
		servlet.writeResult(result, response);

		verify(response).setStatus(RpcServlet.APPLICATION_EXC_STATUS);
		verify(response).setContentType(RpcServlet.JSON_CONTENT_TYPE);
	}

	@Test
	public void testWriteRpcException() throws Exception {
		RpcException exception = RpcException.methodNotFound("Method not found");
		HttpServletResponse response = mockResponse();
		servlet.writeRpcException(exception, response);

		verify(response).setStatus(HttpURLConnection.HTTP_NOT_FOUND);
		verify(response).setContentType(RpcServlet.TEXT_CONTENT_TYPE);
	}

	private HttpServletResponse mockResponse() {
		return mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
	}
}
