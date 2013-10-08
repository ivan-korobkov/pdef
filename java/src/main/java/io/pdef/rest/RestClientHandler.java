package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationResult;
import io.pdef.rpc.*;
import io.pdef.types.*;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class RestClientHandler implements Function<Invocation, InvocationResult> {
	private final Function<RestRequest, RestResponse> sender;

	/** Creates a REST client. */
	public RestClientHandler(final Function<RestRequest, RestResponse> sender) {
		this.sender = checkNotNull(sender);
	}

	@Override
	public InvocationResult apply(final Invocation invocation) {
		return invoke(invocation);
	}

	/** Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception. */
	public InvocationResult invoke(final Invocation invocation) {
		RestRequest request = createRequest(invocation);
		RestResponse response = sendRequest(request);

		if (isSuccessful(response)) {
			return parseResult(response, invocation);
		}

		throw parseError(response);
	}

	/** Converts an invocation into a rest request. */
	@VisibleForTesting
	RestRequest createRequest(final Invocation invocation) {
		RestRequest request;

		if (invocation.getMethod().isPost()) {
			request = RestRequest.post();
		} else {
			request = RestRequest.get();
		}

		for (Invocation inv : invocation.toChain()) {
			serializeInvocation(request, inv);
		}

		return request;
	}

	/** Adds a single invocation to a rest request. */
	@VisibleForTesting
	void serializeInvocation(final RestRequest request, final Invocation invocation) {
		InterfaceMethod method = invocation.getMethod();
		request.appendPath("/");
		if (!method.isIndex()) {
			request.appendPath(Rest.urlencode(method.getName()));
		}

		Object[] args = invocation.getArgs();
		List<InterfaceMethodArg> argds = invocation.getMethod().getArgs();
		assert args.length == argds.size();  // Must be checked in Invocation constructor.

		if (method.isPost()) {
			// Add arguments as post params, serialize messages and collections into json.
			// Post methods are remote.
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				InterfaceMethodArg argd = argds.get(i);

				serializeQueryArg(argd, arg, request.getPost());
			}

		} else if (method.isRemote()) {
			// Add arguments as query params.
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				InterfaceMethodArg argd = argds.get(i);

				serializeQueryArg(argd, arg, request.getQuery());
			}

		} else {
			// Positionally prepend all arguments to the path;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				InterfaceMethodArg argd = argds.get(i);

				request.appendPath("/");
				request.appendPath(serializePositionalArg(argd, arg));
			}
		}
	}

	/** Serializes a positional arg and urlencodes it. */
	@VisibleForTesting
	String serializePositionalArg(final InterfaceMethodArg argd, final Object arg) {
		String serialized = serializeArgToString(argd.getType(), arg);
		return Rest.urlencode(serialized);
	}

	/** Serializes a query/post argument and puts it into a dst map. */
	@VisibleForTesting
	void serializeQueryArg(final InterfaceMethodArg argd, final Object arg,
			final Map<String, String> dst) {
		if (arg == null) {
			return;
		}

		DataType d = argd.getType();
		MessageType md = (MessageType) d;
		boolean isForm = md != null && md.isForm();

		if (!isForm) {
			// Serialize as a normal argument.
			String serialized = serializeArgToString(d, arg);
			dst.put(argd.getName(), serialized);
			return;
		}

		// It's a form, expand its fields into distinct arguments.
		for (MessageField fd : md.getFields()) {
			Object fvalue = fd.get(arg);
			if (fvalue == null) {
				continue;
			}

			String serialized = serializeArgToString(fd.getType(), fvalue);
			dst.put(fd.getName(), serialized);
		}
	}

	/** Serializes primitives and enums to strings and other types to json. */
	@VisibleForTesting
	String serializeArgToString(final DataType descriptor, final Object arg) {
		if (arg == null) {
			return "";
		}

		return descriptor.toString(arg);
	}

	/** Sends a rest request and returns a rest response. */
	@VisibleForTesting
	RestResponse sendRequest(final RestRequest request) {
		return sender.apply(request);
	}

	@VisibleForTesting
	boolean isSuccessful(final RestResponse response) {
		return (response.hasOkStatus() && response.hasJsonContentType());
	}

	@VisibleForTesting
	InvocationResult parseResult(final RestResponse response, final Invocation invocation) {
		RpcResult rpc = RpcResult.parseJson(response.getContent());
		Object result = rpc.getData();
		RpcStatus status = rpc.getStatus();

		if (status == RpcStatus.OK) {
			// It's a successful result.
			// Parse it using the invocation method result type.
			DataType d = (DataType) invocation.getResult();
			Object r = d.parseNative(result);
			return InvocationResult.ok(r);

		} else if (status == RpcStatus.EXCEPTION) {
			// It's an expected application exception.
			// Parse it using the invocation exception type.

			MessageType d = invocation.getExc();
			if (d == null) {
				throw new ClientError().setText("Unsupported application exception");
			}

			// All application exceptions are runtime.
			RuntimeException r = (RuntimeException) d.parseNative(result);
			return InvocationResult.exc(r);
		}

		throw new ClientError().setText("Unsupported rpc response status=" + status);
	}

	/** Parses an rpc error from a rest response via its status. */
	@VisibleForTesting
	RpcError parseError(final RestResponse response) {
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
