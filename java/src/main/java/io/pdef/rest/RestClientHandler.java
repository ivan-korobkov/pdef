package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;

import java.net.HttpURLConnection;

public class RestClientHandler implements Function<Invocation, InvocationResult> {
	private final Function<RestRequest, RestResponse> sender;
	private final RestFormat format;

	/** Creates a REST client. */
	public RestClientHandler(final Function<RestRequest, RestResponse> sender) {
		this.sender = checkNotNull(sender);
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
		RestResponse response = sender.apply(request);

		if (isOkJsonResponse(response)) {
			return format.parseInvocationResult(response,
					invocation.getDataResult(),
					invocation.getExc());
		}

		throw parseRestError(response);
	}

	@VisibleForTesting
	boolean isOkJsonResponse(final RestResponse response) {
		return (response.hasOkStatus() && response.hasJsonContentType());
	}

	/** Parses an rpc error from a rest response via its status. */
	@VisibleForTesting
	RpcError parseRestError(final RestResponse response) {
		int status = response.getStatus();
		String text = Strings.nullToEmpty(response.getContent());

		// Limit the text length to use it in an exception.
		if (text.length() > 255) {
			text = text.substring(0, 255);
		}

		// Map status to exception classes.
		switch (status) {
			case HttpURLConnection.HTTP_BAD_REQUEST:	// 400
				return new ClientError().setText(text);

			case HttpURLConnection.HTTP_NOT_FOUND:		// 404
				return new MethodNotFoundError().setText(text);

			case HttpURLConnection.HTTP_BAD_METHOD:		// 405
				return new MethodNotAllowedError().setText(text);

			case HttpURLConnection.HTTP_BAD_GATEWAY: 	// 502
			case HttpURLConnection.HTTP_UNAVAILABLE:	// 503
				return new ServiceUnavailableError().setText(text);

			case HttpURLConnection.HTTP_INTERNAL_ERROR:	// 500
				return new ServerError().setText(text);

			default:
				return new ServerError()
						.setText("Server error, status=" + status + ", text=" + text);
		}
	}
}
