package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.Clients;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;

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
		RestResult<Integer, ?> result = RestFormat.resultDescriptor(Descriptors.int32, null)
				.newInstance()
				.setSuccess(true)
				.setData(3);

		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(result.serializeToJson(true));

		when(sender.apply(request)).thenReturn(response);
		int methodResult = proxy(handler).indexMethod(1, 2);

		assertEquals(3, methodResult);
	}

	// isOkJsonResponse.

	@Test
	public void testIsSuccessful_ok() throws Exception {
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType();

		assertTrue(handler.isOkJsonResponse(response));
	}

	@Test
	public void testIsSuccessful_error() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(123)
				.setJsonContentType();

		assertFalse(handler.isOkJsonResponse(response));
	}

	// parseRestError.

	@Test
	public void testParseError_clientError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_REQUEST)
				.setContent("Bad request");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new ClientError()
				.setText("Bad request");

		assertEquals(expected, error);
	}

	@Test
	public void testParseError_methodNotFound() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_NOT_FOUND)
				.setContent("Method not found");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new MethodNotFoundError()
				.setText("Method not found");

		assertEquals(expected, error);
	}

	@Test
	public void testParseError_methodNotAllowed() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_METHOD)
				.setContent("Method not allowed");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new MethodNotAllowedError()
				.setText("Method not allowed");

		assertEquals(expected, error);
	}

	@Test
	public void testParseError_serviceUnavailable() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_UNAVAILABLE)
				.setContent("Service unavailable");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new ServiceUnavailableError()
				.setText("Service unavailable");

		assertEquals(expected, error);
	}

	@Test
	public void testParseError_serverError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(12345)
				.setContent("Strange error");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new ServerError()
				.setText("Server error, status=12345, text=Strange error");

		assertEquals(expected, error);
	}

	private TestInterface proxy(final Function<Invocation, InvocationResult> handler) {
		return Clients.client(TestInterface.class, handler);
	}
}
