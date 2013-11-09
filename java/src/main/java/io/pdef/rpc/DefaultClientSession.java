package io.pdef.rpc;

import io.pdef.descriptors.ValueDescriptor;
import io.pdef.formats.JsonFormat;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

public class DefaultClientSession implements ClientSession {
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final int APPLICATION_EXC_STATUS = 422;
	private final String url;
	private final JsonFormat format = JsonFormat.getInstance();

	DefaultClientSession(final String url) {
		this.url = url;
	}

	@Override
	public <T, E> T send(final RpcRequest rpcRequest,
			final ValueDescriptor<T> resultDescriptor,
			final ValueDescriptor<E> excDescriptor) throws Exception {
		Request request = buildRequest(rpcRequest);
		return request.execute().handleResponse(new ResponseHandler<T>() {
			@Override
			public T handleResponse(final HttpResponse response) throws IOException {
				return handle(response, resultDescriptor, excDescriptor);
			}
		});
	}

	// VisibleForTesting
	<T, E> T handle(final HttpResponse response,
			final ValueDescriptor<T> resultDescriptor,
			final ValueDescriptor<E> excDescriptor) throws IOException {
		int status = response.getStatusLine().getStatusCode();
		HttpEntity entity = response.getEntity();

		if (status == HttpURLConnection.HTTP_OK) {
			// It's a successful response.
			return format.fromJson(entity.getContent(), resultDescriptor);

		} else if (status == APPLICATION_EXC_STATUS) {
			// It's an expected application exception.
			if (excDescriptor == null) {
				throw new RpcException(APPLICATION_EXC_STATUS, "Unsupported application exception");
			}
			E exc = format.fromJson(entity.getContent(), excDescriptor);
			throw (RuntimeException) exc;

		}

		String error = entity == null ? "Error"  : EntityUtils.toString(entity);
		throw new RpcException(response.getStatusLine().getStatusCode(), error);
	}

	// VisibleForTesting
	Request buildRequest(final RpcRequest rpcRequest) throws URISyntaxException {
		URI uri = buildUri(rpcRequest);
		Request request = rpcRequest.isPost() ? Request.Post(uri) : Request.Get(uri);
		if (!rpcRequest.isPost()) {
			return request;
		}

		Form form = Form.form();
		for (Map.Entry<String, String> entry : rpcRequest.getPost().entrySet()) {
			form.add(entry.getKey(), entry.getValue());
		}
		request.bodyForm(form.build(), UTF_8);
		return request;
	}

	// VisibleForTesting
	URI buildUri(final RpcRequest rpcRequest) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(url + rpcRequest.getPath());

		for (Map.Entry<String, String> entry : rpcRequest.getQuery().entrySet()) {
			uriBuilder.addParameter(entry.getKey(), entry.getValue());
		}
		return uriBuilder.build();
	}
}
