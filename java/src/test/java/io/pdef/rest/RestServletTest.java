package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;

public class RestServletTest {
	@Mock RestHandler handler;
	RestServlet server;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		server = new RestServlet(handler);
	}

	@Test
	public void testParseRequest() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn(RestRequest.GET);
		when(request.getServletPath()).thenReturn("/my/app");
		when(request.getPathInfo()).thenReturn("/method1/method2");
		when(request.getParameterMap()).thenReturn(ImmutableMap.of(
				"key0", new String[]{"value0"},
				"key1", new String[]{"value1", "value11"}));

		RestRequest req = server.getRestRequest(request);
		assert req.getMethod().equals(RestRequest.GET);
		assert req.getPath().equals("/my/app/method1/method2");
		assert req.getQuery().equals(ImmutableMap.of("key0", "value0", "key1", "value1"));
		assert req.getPost().equals(ImmutableMap.of("key0", "value0", "key1", "value1"));
	}

	@Test
	public void testWriteResponse() throws Exception {
		RestResponse resp = new RestResponse()
				.setOkStatus()
				.setTextContentType()
				.setContent("Привет, мир!");

		HttpServletResponse response = mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
		server.writeResponse(resp, response);

		verify(response).setStatus(HttpURLConnection.HTTP_OK);
		verify(response).setContentType(RestResponse.TEXT_CONTENT_TYPE);
		verify(response.getWriter()).print(resp.getContent());
	}
}
