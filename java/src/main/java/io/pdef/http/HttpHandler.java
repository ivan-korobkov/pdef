package io.pdef.http;

import io.pdef.Pdef;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpHandler<T> {
	private final T service;
	private final PdefInterface iface;

	private final HttpRequestReader reader;
	private final HttpResponseWriter writer;

	public HttpHandler(final T service, final Class<T> cls, final Pdef pdef) {
		this.service = checkNotNull(service);
		this.iface = (PdefInterface) pdef.get(cls);

		reader = new HttpRequestReader(cls, pdef);
		writer = new HttpResponseWriter(pdef);
	}

	public void handle(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) throws IOException {
		Request request = reader.read(httpRequest);
		//FluentFuture<Response> future = iface.invokeRequestAsync(service, request);
		//Response response = future.getUnchecked();
		Response response = iface.invokeRequest(service, request);
		writer.write(response, httpResponse);
	}
}
