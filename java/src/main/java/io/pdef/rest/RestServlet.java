package io.pdef.rest;

import io.pdef.formats.JsonFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class RestServlet<T> extends HttpServlet {
	public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	public static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";
	public static final int APPLICATION_EXC_STATUS = 422;

	private final RestHandler<T> handler;
	private final JsonFormat format = JsonFormat.getInstance();

	public RestServlet(final RestHandler<T> handler) {
		if (handler == null) throw new NullPointerException("handler");
		this.handler = handler;
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		if (req == null) throw new NullPointerException("request");
		if (resp == null) throw new NullPointerException("response");

		RestRequest request = getRestRequest(req);
		try {
			RestResult<?> result = handler.handle(request);
			writeResult(result, resp);
		} catch (RestException e) {
			writeRestException(e, resp);
		} catch (Exception e) {
			writeServerError(e, resp);
		}
	}

	// VisibleForTesting
	RestRequest getRestRequest(final HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getPathInfo();
		Map<String, String> params = getParams(request);

		// In servlets we cannot distinguish between query and post params,
		// so we use the same map for both. It is safe because Pdef REST protocol
		// always uses only one of them.

		return new RestRequest()
				.setMethod(method)
				.setPath(path)
				.setQuery(params)
				.setPost(params);
	}

	private <T, E> void writeResult(final RestResult<T> result, final HttpServletResponse resp)
			throws IOException {
		if (result.isOk()) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType(JSON_CONTENT_TYPE);
		} else {
			resp.setStatus(APPLICATION_EXC_STATUS);
			resp.setContentType(JSON_CONTENT_TYPE);
		}

		PrintWriter writer = resp.getWriter();
		format.toJson(writer, result.getData(), result.getDescriptor(), true);
		writer.flush();
	}

	private void writeRestException(final RestException e, final HttpServletResponse resp)
			throws IOException {
		String message = e.getMessage() != null ? e.getMessage() : "Client error";

		resp.setStatus(e.getStatus());
		resp.setContentType(TEXT_CONTENT_TYPE);
		resp.getWriter().write(message);
	}

	private void writeServerError(final Exception e, final HttpServletResponse resp)
			throws IOException {
		resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		resp.setContentType(TEXT_CONTENT_TYPE);
		resp.getWriter().write("Internal server error");
	}

	private Map<String, String> getParams(final HttpServletRequest request) {
		Map<String, String> params = new HashMap<String, String>();

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
}
