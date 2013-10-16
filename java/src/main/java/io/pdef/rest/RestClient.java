package io.pdef.rest;

import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.InvocationResult;
import io.pdef.invoke.Invoker;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

public class RestClient implements Invoker {
	protected static final Charset CHARSET = Charset.forName("UTF-8");
	private final RestProtocol protocol;
	private final RestHandler handler;

	private RestClient(final RestHandler handler) {
		if (handler == null) throw new NullPointerException("handler");
		this.protocol = new RestProtocol();
		this.handler = handler;
	}

	/** Creates a REST client. */
	public static RestClient create(final String url) {
		RestHandler handler = new DefaultHandler(url, null);
		return new RestClient(handler);
	}

	/** Creates a REST client. */
	public static RestClient create(final String url, @Nullable final RestClientSession session) {
		RestHandler handler = new DefaultHandler(url, session);
		return new RestClient(handler);
	}

	/** Creates a REST client. */
	public static RestClient create(final RestHandler handler) {
		return new RestClient(handler);
	}

	/** Creates a proxy backed by a REST client. */
	public static <T> T create(final Class<T> interfaceClass, final String url) {
		RestClient client = create(url);
		return InvocationProxy.create(interfaceClass, client);
	}

	/** Creates a proxy backed by a REST client. */
	public static <T> T create(final Class<T> interfaceClass, final String url,
			final RestClientSession session) {
		RestClient client = create(url, session);
		return InvocationProxy.create(interfaceClass, client);
	}

	/**
	 * Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception.
	 * */
	@Override
	public InvocationResult invoke(final Invocation invocation) throws Exception {
		if (invocation == null) throw new NullPointerException("invocation");

		RestRequest request = protocol.serializeInvocation(invocation);
		RestResponse response = handler.handle(request);

		if (response.hasOkStatus() && response.hasJsonContentType()) {
			return protocol.parseInvocationResult(response,
					invocation.getDataResult(),
					invocation.getExc());
		} else {
			throw errorResponse(response);
		}
	}

	// VisibleForTesting
	RestException errorResponse(final RestResponse response) {
		int status = response.getStatus();
		String text = response.getContent();
		text = text != null ? text : "";

		// Limit the text length to use it in an exception.
		if (text.length() > 512) {
			text = text.substring(0, 512);
		}

		return new RestException(status, text);
	}

	// VisibleForTesting.
	static class DefaultHandler implements RestHandler {
		protected final String url;
		protected final RestClientSession session;

		DefaultHandler(final String url, @Nullable final RestClientSession session) {
			if (url == null) throw new NullPointerException("url");

			this.url = url;
			this.session = session != null ? session : new DefaultSession();
		}
		@Override
		public RestResponse handle(final RestRequest request) throws Exception {
			Request req = createHttpRequest(request);
			req.version(HttpVersion.HTTP_1_1);
			Response resp = session.send(req);
			return parseRestResponse(resp);
		}

		/** Creates a fluent http client request from a rest request. */
		// VisibleForTesting
		Request createHttpRequest(final RestRequest request) {
			URI uri = buildUri(request);
			if (!request.isPost()) {
				return Request.Get(uri);
			}

			Form form = Form.form();
			for (Map.Entry<String, String> entry : request.getPost().entrySet()) {
				form.add(entry.getKey(), entry.getValue());
			}

			return Request.Post(uri).bodyForm(form.build(), CHARSET);
		}

		/** Creates a URI from a rest request. */
		// VisibleForTesting
		protected URI buildUri(final RestRequest request) {
			String url = joinUrl(request.getPath());
			try {
				URIBuilder builder = new URIBuilder(url);
				for (Map.Entry<String, String> entry : request.getQuery().entrySet()) {
					builder.addParameter(entry.getKey(), entry.getValue());
				}

				return builder.build();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

		/** Joins the base url and the path, deduplicates slashes,
		 * i.e. "http://localhost/" + "/path" => "http://localhost/path". */
		protected String joinUrl(final String path) {
			String url = this.url;
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}

			return path.startsWith("/") ? url + path : url + "/" + path;
		}

		/** Parses a fluent http client response into a rest response. */
		private RestResponse parseRestResponse(final Response resp) throws IOException {
			return resp.handleResponse(new ResponseHandler<RestResponse>() {
				@Override
				public RestResponse handleResponse(final HttpResponse response) throws IOException {
					return parseHttpResponse(response);
				}
			});
		}

		// VisibleForTesting
		RestResponse parseHttpResponse(final HttpResponse resp) throws IOException {
			int status = resp.getStatusLine().getStatusCode();
			String content = null;
			String contentType = null;

			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				contentType = ContentType.getOrDefault(entity).getMimeType();
				content = EntityUtils.toString(entity);
			}

			return new RestResponse()
					.setStatus(status)
					.setContentType(contentType)
					.setContent(content);
		}
	}

	static class DefaultSession implements RestClientSession {
		@Override
		public Response send(final Request request) throws IOException {
			return request.execute();
		}
	}
}
