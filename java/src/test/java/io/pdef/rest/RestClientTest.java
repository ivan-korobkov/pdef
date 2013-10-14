package io.pdef.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationClient;
import io.pdef.invoke.InvocationResult;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.HttpURLConnection;

public class RestClientTest {
	@Mock Function<RestRequest, RestResponse> requestHandler;
	RestClient handler;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		handler = RestClient.client(requestHandler);
	}

	@Test
	public void testInvoke() throws Exception {
		RestRequest request = new RestRequest()
				.setMethod(RestRequest.GET)
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

		when(requestHandler.apply(request)).thenReturn(response);
		int methodResult = proxy(handler).indexMethod(1, 2);

		assertEquals(3, methodResult);
	}

	@Test
	public void testParseErrorResponse() throws Exception {
		RestResponse response = new RestResponse()
				.setStatus(HttpURLConnection.HTTP_BAD_REQUEST)
				.setContent("Bad request");

		RestException exception = handler.parseErrorResponse(response);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, exception.getStatus());
		assertEquals("Bad request", exception.getMessage());
	}

	private TestInterface proxy(final Function<Invocation, InvocationResult> handler) {
		return InvocationClient.create(TestInterface.class, handler);
	}
}
