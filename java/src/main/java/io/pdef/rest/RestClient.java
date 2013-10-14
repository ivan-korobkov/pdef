package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationClient;
import io.pdef.invoke.InvocationResult;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

public class RestClient implements Function<Invocation, InvocationResult> {
	private final Function<RestRequest, RestResponse> session;
	private final RestFormat format;

	public static Builder builder() {
		return new Builder();
	}

	private RestClient(final Function<RestRequest, RestResponse> session) {
		this.session = checkNotNull(session);
		format = new RestFormat();
	}

	/**
	 * Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception.
	 * */
	@Override
	public InvocationResult apply(final Invocation invocation) {
		checkNotNull(invocation);

		RestRequest request = format.serializeInvocation(invocation);
		RestResponse response = session.apply(request);
		assert response != null;

		if (response.hasOkStatus() && response.hasJsonContentType()) {
			return format.parseInvocationResult(response,
					invocation.getDataResult(),
					invocation.getExc());
		} else {
			throw parseErrorResponse(response);
		}
	}

	@VisibleForTesting
	RestException parseErrorResponse(final RestResponse response) {
		assert response != null;
		int status = response.getStatus();
		String text = Strings.nullToEmpty(response.getContent());

		// Limit the text length to use it in an exception.
		if (text.length() > 512) {
			text = text.substring(0, 512);
		}

		return new RestException(status, text);
	}

	public static class Builder {
		private String url;
		private Function<Request, Response> session;
		private Function<RestRequest, RestResponse> rawSession;

		private Builder() {}

		public String getUrl() {
			return url;
		}

		public Builder setUrl(final String url) {
			checkState(rawSession == null, "Cannot set a url, a rawSession is already present");
			this.url = checkNotNull(url);
			return this;
		}

		public Function<Request, Response> getSession() {
			return session;
		}

		public Builder setSession(final Function<Request, Response> session) {
			checkState(rawSession == null, "Cannot set a session, a rawSession is already present");
			this.session = checkNotNull(session);
			return this;
		}

		public Function<RestRequest, RestResponse> getRawSession() {
			return rawSession;
		}

		public Builder setRawSession(final Function<RestRequest, RestResponse> rawSession) {
			checkState(url == null && session == null,
					"Cannot set a rawSession, a url or a session is present");
			this.rawSession = checkNotNull(rawSession);
			return this;
		}

		private Function<RestRequest, RestResponse> buildSession() {
			checkState(url != null || rawSession != null, "URL or rawSession must be present");

			if (url != null) {
				return new RestClientHttpSession(url, session);
			} else {
				return rawSession;
			}
		}

		/** Creates a raw client. */
		public RestClient build() {
			Function<RestRequest, RestResponse> session = buildSession();
			return new RestClient(session);
		}

		/** Creates a proxy client. */
		public <T> T buildProxy(final Class<T> interfaceClass) {
			checkNotNull(interfaceClass);
			RestClient raw = build();
			return InvocationClient.create(interfaceClass, raw);
		}
	}
}
