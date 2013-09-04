package pdef.rest;

import com.google.common.base.Function;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

class RestClientHttpSender implements Function<RestRequest, RestResponse> {
	private final String url;
	private final Function<Request, Response> session;

	RestClientHttpSender(final String url) {
		this.url = checkNotNull(url);
		this.session = new DefaultSession(this);
	}

	RestClientHttpSender(final String url, final Function<Request, Response> session) {
		this.url = checkNotNull(url);
		this.session = checkNotNull(session);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		Request req = createRequest(request);
		Response resp = sendRequest(req);
		return parseResponse(resp);
	}

	/** Creates a fluent http client request from a rest request. */
	private Request createRequest(final RestRequest request) {
		URI uri = buildUri(request);
		if (!request.isPost()) {
			return Request.Get(uri);
		}

		Form form = Form.form();
		for (Map.Entry<String, String> entry : request.getPost().entrySet()) {
			form.add(entry.getKey(), entry.getValue());
		}

		return Request.Post(uri).bodyForm(form.build());
	}

	/** Creates a URI from a rest request. */
	private URI buildUri(final RestRequest request) {
		String url = getUrl(request.getPath());
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

	/** Joins the base url and the path. */
	private String getUrl(final String path) {
		return this.url + path;
	}

	/** Sends a fluent http client request and returns a response. */
	private Response sendRequest(final Request req) {
		return session.apply(req);
	}

	private Response doSendRequest(final Request request) {
		try {
			return request.execute();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	private RestResponse parseHttpResponse(final HttpResponse resp) throws IOException {
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
		private final RestClientHttpSender sender;

		private DefaultSession(final RestClientHttpSender sender) {
			this.sender = sender;
		}

		@Override
		public Response apply(final Request request) {
			return sender.doSendRequest(request);
		}
	}
}
