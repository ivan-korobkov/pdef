package pdef.rest;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

public class ServletRestServer {
	private final Function<RestRequest, RestResponse> restServer;

	/** Creates a servlet rest server. */
	public ServletRestServer(final Function<RestRequest, RestResponse> restServer) {
		this.restServer = checkNotNull(restServer);
	}

	public void handle(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		checkNotNull(request);

		RestRequest req = parseRequest(request);
		RestResponse resp = handleRequest(req);
		writeResponse(resp, response);
	}

	private RestRequest parseRequest(final HttpServletRequest request) {
		String method = request.getMethod();

		// In servlets we cannot distinguish between query and post params,
		// so we use the same map for both. It is safe because Pdef REST protocol
		// always uses only one of them.
		String path = parsePath(request);
		Map<String, String> params = parseParams(request);
		return new RestRequest()
				.setMethod(method)
				.setPath(path)
				.setQuery(params)
				.setPost(params);
	}

	private String parsePath(final HttpServletRequest request) {
		String servletPath = Strings.nullToEmpty(request.getServletPath());
		String pathInfo = Strings.nullToEmpty(request.getPathInfo());
		return servletPath + pathInfo;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> parseParams(final HttpServletRequest request) {
		Map<String, String> params = Maps.newHashMap();

		Enumeration<String> names = request.getParameterNames();
		if (names == null) {
			return params;
		}

		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = request.getParameter(name);
			params.put(name, value);
		}

		return params;
	}

	private RestResponse handleRequest(final RestRequest req) {
		return restServer.apply(req);
	}

	private void writeResponse(final RestResponse resp, final HttpServletResponse response)
			throws IOException {
		response.setStatus(resp.getStatus());
		response.setContentType(resp.getContentType());
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append(resp.getContent());
	}
}
