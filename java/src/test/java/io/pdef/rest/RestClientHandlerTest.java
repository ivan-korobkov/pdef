package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.Clients;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.test.interfaces.TestInterface;
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
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType()
				.setContent(new RpcResult().setStatus(RpcStatus.OK).setData(3).serializeToJson());

		when(sender.apply(request)).thenReturn(response);
		int result = proxy(handler).indexMethod(1, 2);

		assert result == 3;
	}

	// isOkJsonResponse.

	@Test
	public void testIsSuccessful_ok() throws Exception {
		RestResponse response = new RestResponse()
				.setOkStatus()
				.setJsonContentType();

		assert handler.isOkJsonResponse(response);
	}

	@Test
	public void testIsSuccessful_error() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(123)
				.setJsonContentType();

		assert !handler.isOkJsonResponse(response);
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

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotFound() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_NOT_FOUND)
				.setContent("Method not found");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new MethodNotFoundError()
				.setText("Method not found");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_methodNotAllowed() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_METHOD)
				.setContent("Method not allowed");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new MethodNotAllowedError()
				.setText("Method not allowed");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serviceUnavailable() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_UNAVAILABLE)
				.setContent("Service unavailable");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new ServiceUnavailableError()
				.setText("Service unavailable");

		assert error.equals(expected);
	}

	@Test
	public void testParseError_serverError() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(12345)
				.setContent("Strange error");

		RpcError error = handler.parseRestError(response);
		RpcError expected = new ServerError()
				.setText("Server error, status=12345, text=Strange error");

		assert error.equals(expected);
	}

	private TestInterface proxy(final Function<Invocation, InvocationResult> handler) {
		return Clients.client(TestInterface.class, handler);
	}
}
