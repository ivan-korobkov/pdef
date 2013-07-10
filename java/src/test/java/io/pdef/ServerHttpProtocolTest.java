package io.pdef;

import com.google.common.base.Function;
import com.google.common.net.MediaType;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;
import io.pdef.rpc.ResponseStatus;
import io.pdef.test.TestInterface;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class ServerHttpProtocolTest {
	@Test
	public void testHandle() throws Exception {
		Function<Request, Response> rpcHandler = new Function<Request, Response>() {
			@Nullable
			@Override
			public Response apply(@Nullable final Request input) {
				return Response.builder()
						.setResult("hello, world")
						.setStatus(ResponseStatus.OK)
						.build();
			}
		};

		Function<ServerHttpProtocol.RequestResponse, Void> handler = Server
				.httpHandler(TestInterface.DESCRIPTOR, rpcHandler);

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
		when(request.getPathInfo()).thenReturn("/interface0/hello/John/Doe");

		handler.apply(new ServerHttpProtocol.RequestResponse(request, response));
		verify(response).setStatus(HttpServletResponse.SC_OK);
		verify(response).setContentType(MediaType.JSON_UTF_8.toString());
		verify(response.getWriter()).write("{\"status\":\"ok\",\"result\":\"hello, world\"}");
	}
}
