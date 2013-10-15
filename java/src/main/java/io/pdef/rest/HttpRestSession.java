package io.pdef.rest;

import io.pdef.Func;
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

class HttpRestSession implements Func<RestRequest, RestResponse> {
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private final String url;
	private final Func<Request, Response> session;

	/** Creates a rest HTTP session. */
	public HttpRestSession(final String url) {
		this(url, null);
	}

	/** Creates a rest HTTP session. */
	public HttpRestSession(final String url, @Nullable final Func<Request, Response> session) {
		if (url == null) throw new NullPointerException("url");
		this.url = url;
		this.session = session != null ? session : new HttpSession();
	}

	@Override
	public RestResponse apply(final RestRequest request) throws Exception {
		Request req = createRequest(request);
		req.version(HttpVersion.HTTP_1_1);
		Response resp = session.apply(req);
		return parseResponse(resp);
	}

	/** Creates a fluent http client request from a rest request. */
	// VisibleForTesting
	Request createRequest(final RestRequest request) {
		URI uri = buildUri(request);
		if (!request.isPost()) {
			return Request.Get(uri);
		}

		Form form = Form.form();
		for (Map.Entry<String, String> entry : request.getPost().entrySet()) {
			form.add(entry.getKey(), entry.getValue());
		}

		return Request.Post(uri).bodyForm(form.build(), UTF_8);
	}

	/** Creates a URI from a rest request. */
	// VisibleForTesting
	URI buildUri(final RestRequest request) {
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
	private String joinUrl(final String path) {
		String url = this.url;
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		return path.startsWith("/") ? url + path : url + "/" + path;
	}

	/** Parses a fluent http client response into a rest response. */
	private RestResponse parseResponse(final Response resp) throws IOException {
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
