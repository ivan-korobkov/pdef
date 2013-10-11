package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.meta.*;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;

import java.net.HttpURLConnection;

public class RestServerHandler implements Function<RestRequest, RestResponse> {
	private final InterfaceType type;
	private final Function<Invocation, InvocationResult> invoker;
	private final RestFormat format;

	/** Creates a REST server handler. */
	public RestServerHandler(final Class<?> cls,
			final Function<Invocation, InvocationResult> invoker) {
		this.type = InterfaceType.findMetaType(cls);
		this.invoker = checkNotNull(invoker);
		format = new RestFormat();

		checkArgument(type != null, "Cannot find an interface type in %s", cls);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		checkNotNull(request);

		try {
			Invocation invocation = format.parseInvocation(request, type);
			InvocationResult result = invoker.apply(invocation);
			return format.serializeInvocationResult(result, invocation);
		} catch (Exception e) {
			// Catch any unhandled (not application-specific) exception and
			// convert it into a server error response.
			return handleException(e);
		}
	}

	@VisibleForTesting
	RestResponse handleException(final Exception e) {
		int httpStatus;
		String content;

		if (e instanceof WrongMethodArgsError) {
			httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
		} else if (e instanceof MethodNotFoundError) {
			httpStatus = HttpURLConnection.HTTP_NOT_FOUND;
		} else if (e instanceof MethodNotAllowedError) {
			httpStatus = HttpURLConnection.HTTP_BAD_METHOD;
		} else if (e instanceof ClientError) {
			httpStatus = HttpURLConnection.HTTP_BAD_REQUEST;
		} else if (e instanceof ServiceUnavailableError) {
			httpStatus = HttpURLConnection.HTTP_UNAVAILABLE;
		} else if (e instanceof ServerError) {
			httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
		} else {
			httpStatus = HttpURLConnection.HTTP_INTERNAL_ERROR;
		}

		if (e instanceof RpcError) {
			content = ((RpcError) e).getText();
		} else {
			content = "Internal server error";
		}

		return new RestResponse()
				.setStatus(httpStatus)
				.setContent(content)
				.setTextContentType();
	}
}
