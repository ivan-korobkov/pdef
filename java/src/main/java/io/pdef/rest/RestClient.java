package io.pdef.rest;

import io.pdef.Func;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationProxy;
import io.pdef.invoke.InvocationResult;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public class RestClient implements Func<Invocation, InvocationResult> {
	private final Func<RestRequest, RestResponse> session;
	private final RestProtocol format;

	public RestClient(final Func<RestRequest, RestResponse> session) {
		if (session == null) throw new NullPointerException("session");

		this.session = session;
		format = new RestProtocol();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception.
	 * */
	@Override
	public InvocationResult apply(final Invocation invocation) throws Exception {
		if (invocation == null) throw new NullPointerException("invocation");

		RestRequest request = format.serializeInvocation(invocation);
		RestResponse response = session.apply(request);

		if (response.hasOkStatus() && response.hasJsonContentType()) {
			return format.parseInvocationResult(response,
					invocation.getDataResult(),
					invocation.getExc());
		} else {
			throw parseErrorResponse(response);
		}
	}

	// VisibleForTesting
	RestException parseErrorResponse(final RestResponse response) {
		int status = response.getStatus();
		String text = response.getContent();
		text = text != null ? text : "";

		// Limit the text length to use it in an exception.
		if (text.length() > 512) {
			text = text.substring(0, 512);
		}

		return new RestException(status, text);
	}

	public static class Builder {
		private String url;
		private Func<Request, Response> session;

		private Builder() {}

		public String getUrl() {
			return url;
		}

		public Builder setUrl(final String url) {
			if (url == null) throw new IllegalArgumentException("url");

			this.url = url;
			return this;
		}

		public Func<Request, Response> getSession() {
			return session;
		}

		public Builder setSession(final Func<Request, Response> session) {
			if (session == null) throw new NullPointerException("session");
			this.session = session;
			return this;
		}

		private Func<RestRequest, RestResponse> buildSession() {
			if (url == null) {
				throw new IllegalStateException("URL must be present");
			}

			return new HttpRestSession(url, session);
		}

		/** Creates a raw client. */
		public RestClient build() {
			Func<RestRequest, RestResponse> session = buildSession();
			return new RestClient(session);
		}

		/** Creates a proxy client. */
		public <T> T buildProxy(final Class<T> interfaceClass) {
			if (interfaceClass == null) throw new NullPointerException("interfaceClass");
			RestClient raw = build();
			return InvocationProxy.create(interfaceClass, raw);
		}
	}
}
