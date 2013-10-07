package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class RestServer {
	private final Function<RestRequest, RestResponse> handler;

	/** Creates a REST server. */
	public RestServer(final Function<RestRequest, RestResponse> handler) {
		this.handler = checkNotNull(handler);
	}

	public void handle(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		checkNotNull(request);

		RestRequest req = parseRequest(request);
		RestResponse resp = handleRequest(req);
		writeResponse(resp, response);
	}

	@VisibleForTesting
	RestRequest parseRequest(final HttpServletRequest request) {
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

	private Map<String, String> parseParams(final HttpServletRequest request) {
		Map<String, String> params = Maps.newHashMap();

		@SuppressWarnings("unchecked")
		Map<String, String[]> map = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();
			if (values == null || values.length == 0) {
				continue;
			}

			String value = values[0];
			params.put(key, value);
		}

		return params;
	}

	private RestResponse handleRequest(final RestRequest req) {
		return handler.apply(req);
	}

	@VisibleForTesting
	void writeResponse(final RestResponse resp, final HttpServletResponse response)
			throws IOException {
		response.setStatus(resp.getStatus());
		response.setContentType(resp.getContentType());
		response.getWriter().print(resp.getContent());
	}
}
