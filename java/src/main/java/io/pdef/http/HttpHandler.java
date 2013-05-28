package io.pdef.http;

import com.google.common.base.Stopwatch;
import io.pdef.rpc.Handler;
import io.pdef.Pdef;
import io.pdef.rpc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpHandler<T> {
	private final HttpRequestReader reader;
	private final HttpResponseWriter writer;
	private final Handler<T> handler;

	public HttpHandler(final T service, final Class<T> cls, final Pdef pdef) {
		reader = new HttpRequestReader(cls, pdef);
		writer = new HttpResponseWriter(pdef);
		handler = new Handler<T>(service, cls, pdef);
	}

	public void handle(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) throws IOException {
		Stopwatch sw = new Stopwatch().start();

		Response response;
		try {
			Request request = reader.read(httpRequest);
			response = handler.handle(request);
		} catch (RpcException e) {
			response = Response.builder()
					.setStatus(ResponseStatus.RPC_ERROR)
					.setRpcExc(e)
					.build();
		} catch (Exception e) {
			RpcException serverError = RpcException.builder()
					.setCode(RpcExceptionCode.SERVER_ERROR)
					.setText("Internal server error")
					.build();

			response = Response.builder()
					.setStatus(ResponseStatus.RPC_ERROR)
					.setRpcExc(serverError)
					.build();
		}

		writer.write(response, httpResponse);
		System.out.println("Done in " + sw);
	}
}
