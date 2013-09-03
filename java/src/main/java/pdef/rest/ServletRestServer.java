package pdef.rest;

import com.google.common.base.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServletRestServer<T> implements Function<HttpServletRequest, HttpServletResponse> {
	private final Function<RestRequest, RestResponse> restServer;

	/** Creates a servlet rest server. */
	public ServletRestServer(final Function<RestRequest, RestResponse> restServer) {
		this.restServer = checkNotNull(restServer);
	}

	@Override
	public HttpServletResponse apply(final HttpServletRequest request) {
		RestRequest req = parseRequest(request);
		RestResponse resp = handleRequest(req);
		return createResponse(resp);
	}

	private RestRequest parseRequest(final HttpServletRequest request) {
		return null;
	}

	private RestResponse handleRequest(final RestRequest req) {
		return null;
	}

	private HttpServletResponse createResponse(final RestResponse resp) {
		return null;
	}
}
