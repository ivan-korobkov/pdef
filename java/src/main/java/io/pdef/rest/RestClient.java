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

import javax.annotation.Nullable;

public class RestClient implements Function<Invocation, InvocationResult> {
	private final Function<RestRequest, RestResponse> requestHandler;
	private final RestFormat format;

	/** Creates a default REST client. */
	public static <T> T httpClient(final Class<T> cls, final String url) {
		checkNotNull(cls);
		checkNotNull(url);

		return httpClient(cls, url, null);
	}

	/** Creates a default REST client with a custom session. */
	public static <T> T httpClient(final Class<T> cls, final String url,
			@Nullable final Function<Request, Response> session) {
		checkNotNull(cls);
		checkNotNull(url);

		RestClientHttpRequestHandler sender = new RestClientHttpRequestHandler(url, session);
		RestClient restClient = new RestClient(sender);
		return InvocationClient.create(cls, restClient);
	}

	/** Creates a rest client. */
	public static RestClient client(final Function<RestRequest, RestResponse> requestHandler) {
		return new RestClient(requestHandler);
	}

	private RestClient(final Function<RestRequest, RestResponse> requestHandler) {
		this.requestHandler = checkNotNull(requestHandler);
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
		RestResponse response = requestHandler.apply(request);
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
}
