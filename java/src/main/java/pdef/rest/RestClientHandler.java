package pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Strings;
import pdef.invocation.Invocation;
import pdef.invocation.InvocationResult;
import pdef.TypeEnum;
import pdef.descriptors.*;
import pdef.rpc.*;

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
		MethodDescriptor method = invocation.getMethod();
		request.appendPath("/");
		if (!method.isIndex()) {
			request.appendPath(Rest.urlencode(method.getName()));
		}

		Object[] args = invocation.getArgs();
		List<ArgDescriptor> argds = invocation.getMethod().getArgs();
		assert args.length == argds.size();  // Must be checked in Invocation constructor.

		if (method.isPost()) {
			// Add arguments as post params, serialize messages and collections into json.
			// Post methods are remote.
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				ArgDescriptor argd = argds.get(i);

				serializeQueryArg(argd, arg, request.getPost());
			}

		} else if (method.isRemote()) {
			// Add arguments as query params.
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				ArgDescriptor argd = argds.get(i);

				serializeQueryArg(argd, arg, request.getQuery());
			}

		} else {
			// Positionally prepend all arguments to the path;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				ArgDescriptor argd = argds.get(i);

				request.appendPath("/");
				request.appendPath(serializePositionalArg(argd, arg));
			}
		}
	}

	/** Serializes a positional arg and urlencodes it. */
	@VisibleForTesting
	String serializePositionalArg(final ArgDescriptor argd, final Object arg) {
		String serialized = serializeArgToString(argd.getType(), arg);
		return Rest.urlencode(serialized);
	}

	/** Serializes a query/post argument and puts it into a dst map. */
	@VisibleForTesting
	void serializeQueryArg(final ArgDescriptor argd, final Object arg,
			final Map<String, String> dst) {
		if (arg == null) {
			return;
		}

		DataDescriptor d = argd.getType();
		MessageDescriptor md = d.asMessageDescriptor();
		boolean isForm = md != null && md.isForm();

		if (!isForm) {
			// Serialize as a normal argument.
			String serialized = serializeArgToString(d, arg);
			dst.put(argd.getName(), serialized);
			return;
		}

		// It's a form, expand its fields into distinct arguments.
		for (FieldDescriptor fd : md.getFields()) {
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
	String serializeArgToString(final DataDescriptor descriptor, final Object arg) {
		if (arg == null) {
			return "";
		}

		TypeEnum type = descriptor.getType();
		if (type.isPrimitive()) {
			return ((PrimitiveDescriptor) descriptor).toString(arg);
		}

		if (type == TypeEnum.ENUM) {
			return ((EnumDescriptor) descriptor).toString(arg);
		}

		return descriptor.toJson(arg, false);
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
			// Parse it using the invocation method result descriptor.
			DataDescriptor d = (DataDescriptor) invocation.getResult();
			Object r = d.parseObject(result);
			return InvocationResult.ok(r);

		} else if (status == RpcStatus.EXCEPTION) {
			// It's an expected application exception.
			// Parse it using the invocation exception descriptor.

			MessageDescriptor d = invocation.getExc();
			if (d == null) {
				throw ClientError.builder()
						.setText("Unsupported application exception")
						.build();
			}

			// All application exceptions are runtime.
			RuntimeException r = (RuntimeException) d.parseObject(result);
			return InvocationResult.exc(r);
		}

		throw ClientError.builder()
				.setText("Unsupported rpc response status=" + status)
				.build();
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
				return ClientError.builder()
						.setText(text)
						.build();

			case HttpURLConnection.HTTP_NOT_FOUND:		// 404
				return MethodNotFoundError.builder()
						.setText(text)
						.build();

			case HttpURLConnection.HTTP_BAD_METHOD:		// 405
				return MethodNotAllowedError.builder()
						.setText(text)
						.build();

			case HttpURLConnection.HTTP_BAD_GATEWAY: 	// 502
			case HttpURLConnection.HTTP_UNAVAILABLE:	// 503
				return ServiceUnavailableError.builder()
						.setText(text)
						.build();

			case HttpURLConnection.HTTP_INTERNAL_ERROR:	// 500
				return ServerError.builder()
						.setText(text)
						.build();

			default:
				return ServerError.builder()
						.setText("Server error, status=" + status + ", text=" + text)
						.build();
		}
	}
}
