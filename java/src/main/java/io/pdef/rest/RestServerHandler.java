package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MessageDescriptor;
import io.pdef.invoke.Invocation;
import io.pdef.invoke.InvocationResult;
import io.pdef.rpc.*;

import java.net.HttpURLConnection;

public class RestServerHandler<T> implements Function<RestRequest, RestResponse> {
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Invocation, InvocationResult> invoker;
	private final RestFormat format;

	public static <T> RestServerHandler<T> create(final Class<T> cls,
			final Function<Invocation, InvocationResult> invoker) {
		return new RestServerHandler<T>(cls, invoker);
	}

	/** Creates a REST server handler. */
	protected RestServerHandler(final Class<T> cls,
			final Function<Invocation, InvocationResult> invoker) {
		this.descriptor = InterfaceDescriptor.findDescriptor(cls);
		this.invoker = checkNotNull(invoker);
		format = new RestFormat();

		checkArgument(descriptor != null, "Cannot find an interface descriptor in %s", cls);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		checkNotNull(request);

		try {
			Invocation invocation = format.parseInvocation(request, descriptor);
			DataDescriptor<?> dataDescriptor = invocation.getDataResult();
			MessageDescriptor<?> excDescriptor = invocation.getExc();

			InvocationResult result = invoker.apply(invocation);
			return format.serializeInvocationResult(result, dataDescriptor, excDescriptor);

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
