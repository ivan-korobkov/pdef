package io.pdef.http;

import com.google.common.base.Function;
import com.google.common.net.MediaType;
import io.pdef.rpc.RpcRequest;
import io.pdef.rpc.RpcResponse;
import io.pdef.rpc.RpcResponseStatus;
import io.pdef.test.TestInterface;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServerTest {
	@Test
	public void testHandle() throws Exception {
		Function<RpcRequest, RpcResponse> rpcHandler = new Function<RpcRequest, RpcResponse>() {
			@Nullable
			@Override
			public RpcResponse apply(@Nullable final RpcRequest input) {
				return RpcResponse.builder()
						.setResult("hello, world")
						.setStatus(RpcResponseStatus.OK)
						.build();
			}
		};

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS);
		when(request.getPathInfo()).thenReturn("/interface0/hello/John/Doe");

		Function<HttpServletRequest, Void> handler = HttpServer
				.requestReader(TestInterface.DESCRIPTOR)
				.then(rpcHandler)
				.then(HttpServer.responseWriter(response));

		handler.apply(request);
		verify(response).setStatus(HttpServletResponse.SC_OK);
		verify(response).setContentType(MediaType.JSON_UTF_8.toString());
		verify(response.getWriter()).write("{\"status\":\"ok\",\"result\":\"hello, world\"}");
	}
}
