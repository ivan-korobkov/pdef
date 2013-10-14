package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
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
import java.util.Map;

class RestClientHttpSession implements Function<RestRequest, RestResponse> {
	private final String url;
	private final Function<Request, Response> session;

	/** Creates a REST client sender. */
	public RestClientHttpSession(final String url,
			@Nullable final Function<Request, Response> session) {
		this.url = checkNotNull(url);
		this.session = session != null ? session : new DefaultSession();
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		Request req = createRequest(request);
		req.version(HttpVersion.HTTP_1_1);
		Response resp = sendRequest(req);
		return parseResponse(resp);
	}

	/** Creates a fluent http client request from a rest request. */
	@VisibleForTesting
	Request createRequest(final RestRequest request) {
		URI uri = buildUri(request);
		if (!request.isPost()) {
			return Request.Get(uri);
		}

		Form form = Form.form();
		for (Map.Entry<String, String> entry : request.getPost().entrySet()) {
			form.add(entry.getKey(), entry.getValue());
		}

		return Request.Post(uri).bodyForm(form.build(), Charsets.UTF_8);
	}

	/** Creates a URI from a rest request. */
	@VisibleForTesting
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

	/** Sends a fluent http client request and returns a response. */
	private Response sendRequest(final Request req) {
		return session.apply(req);
	}

	/** Parses a fluent http client response into a rest response. */
	private RestResponse parseResponse(final Response resp) {
		try {
			return resp.handleResponse(new ResponseHandler<RestResponse>() {
				@Override
				public RestResponse handleResponse(final HttpResponse response) throws IOException {
					return parseHttpResponse(response);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@VisibleForTesting
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

	private static class DefaultSession implements Function<Request, Response> {
		@Override
		public Response apply(final Request request) {
			try {
				return request.execute();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
