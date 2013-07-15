package io.pdef.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

/** Combines an http request and a response into one function arg. */
public class HttpRequestResponse {
	private final HttpServletRequest request;
	private final HttpServletResponse response;

	public HttpRequestResponse(final HttpServletRequest request,
			final HttpServletResponse response) {
		this.request = checkNotNull(request);
		this.response = checkNotNull(response);
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}
}
