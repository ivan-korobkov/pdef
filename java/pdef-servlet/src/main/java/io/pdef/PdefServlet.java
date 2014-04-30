/*
 * Copyright: 2013 Pdef <http://pdef.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class PdefServlet<T> extends HttpServlet {
	static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

	private boolean indentJson;
	private final transient PdefHandler<T> server;

	public PdefServlet(final Class<T> iface, final T server) {
		this(new PdefHandler<T>(iface, server));
	}

	public PdefServlet(final PdefHandler<T> server) {
		if (server == null) throw new NullPointerException("server");
		this.server = server;
	}

	public PdefServlet<T> indentJson(final boolean indentJson) {
		this.indentJson = indentJson;
		return this;
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		if (req == null) throw new NullPointerException("request");
		if (resp == null) throw new NullPointerException("response");

		PdefRequest request = readRequest(req);
		PdefResponse<?> response;
		try {
			response = server.handle(request);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(JSON_CONTENT_TYPE);
		response.toJson(resp.getWriter(), indentJson);
		resp.flushBuffer();
	}

	// VisibleForTesting
	PdefRequest readRequest(final HttpServletRequest request) {
		String method = request.getMethod();
		String relativePath = getRelativePath(request);

		// In servlets we cannot distinguish between query and post params,
		// so we use the same map for both. It is safe because Pdef HTTP RPC
		// always uses only one of them.
		Map<String, String> params = getParams(request);

		return new PdefRequest()
				.setMethod(method)
				.setRelativePath(relativePath)
				.setQuery(params)
				.setPost(params);
	}

	String getRelativePath(final HttpServletRequest request) {
		// It is voodoo magic.
		// I consulted the servlet specs to write it. 
		// But I don't remember how it works.
		
		@Nullable
		String pathInfo = request.getPathInfo();
		String contextPath = nullToEmpty(request.getContextPath());
		String servletPath = nullToEmpty(request.getServletPath());
		String basePath = pathInfo == null ? contextPath + "/" : contextPath + servletPath + "/";

		String relativePath = request.getRequestURI();
		if (basePath.length() > relativePath.length()) {
			relativePath = "";
		} else {
			relativePath = relativePath.substring(basePath.length());
		}
		
		if (relativePath.isEmpty()) {
			relativePath = "/";
		}
		return relativePath.charAt(0) == '/' ? relativePath : '/' + relativePath;
	}

	Map<String, String> getParams(final HttpServletRequest request) {
		Map<String, String> result = new HashMap<String, String>();
		Map<String, String[]> params = request.getParameterMap();

		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();
			if (values == null || values.length == 0) {
				continue;
			}

			String value = values[0];
			result.put(key, value);
		}

		return result;
	}

	private static String nullToEmpty(final String s) {
		return s == null ? "" : s;
	}
}
