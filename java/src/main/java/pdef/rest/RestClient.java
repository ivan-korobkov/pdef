package pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import pdef.Invocation;
import pdef.TypeEnum;
import pdef.descriptors.*;
import pdef.rpc.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RestClient implements Function<Invocation, Object> {
	private final Function<RestRequest, RestResponse> sender;

	/** Creates a rest client with the default rest client http sender. */
	public RestClient(final String url) {
		this(new RestClientHttpSender(url));
	}

	/** Creates a rest client with a given sender. */
	public RestClient(final Function<RestRequest, RestResponse> sender) {
		this.sender = checkNotNull(sender);
	}

	@Override
	public Object apply(final Invocation invocation) {
		return invoke(invocation);
	}

	/** Serializes an invocation, sends a rest request, parses a rest response,
	 * and returns the result or raises an exception. */
	public Object invoke(final Invocation invocation) {
		RestRequest request = createRequest(invocation);
		RestResponse response = sendRequest(request);

		if (!isSuccess(response)) {
			throw parseError(response);
		}

		RpcResponse rpc = parseRpcResponse(response);
		Object result = rpc.getResult();
		RpcStatus status = rpc.getStatus();
		switch (status) {
			case OK:
				return parseRpcResult(result, invocation);
			case EXCEPTION:
				throw parseRpcException(result, invocation);
			default:
				throw ClientError.builder()
						.setText("Unknown rpc response status: " + status)
						.build();
		}
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
	private void serializeInvocation(final RestRequest request, final Invocation invocation) {
		MethodDescriptor method = invocation.getMethod();
		request.appendPath("/");
		if (!method.isIndex()) {
			request.appendPath(urlencode(method.getName()));
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
	private String serializePositionalArg(final ArgDescriptor argd, final Object arg) {
		String serialized = serializeArgToString(argd.getType(), arg);
		return urlencode(serialized);
	}

	/** Serializes a query/post argument and puts it into a dst map. */
	private void serializeQueryArg(final ArgDescriptor argd, final Object arg,
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
	private String serializeArgToString(final DataDescriptor descriptor, final Object arg) {
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

		return descriptor.toJson(arg);
	}

	/** Sends a rest request and returns a rest response. */
	@VisibleForTesting
	RestResponse sendRequest(final RestRequest request) {
		return sender.apply(request);
	}

	/** Checks that a rest response has 200 OK status and application/json content type. */
	@VisibleForTesting
	boolean isSuccess(final RestResponse response) {
		return response.isOK() && response.isApplicationJson();
	}

	/** Parses an rpc error from a rest response via its status. */
	@VisibleForTesting
	RuntimeException parseError(final RestResponse response) {
		int status = response.getStatus();
		String text = response.getContent();

		switch (status) {
			case 400:
				throw ClientError.builder()
						.setText(text)
						.build();
			case 404:
				throw MethodNotFoundError.builder()
						.setText(text)
						.build();
			case 405:
				throw MethodNotAllowedError.builder()
						.setText(text)
						.build();
			case 502:
			case 503:
				throw ServiceUnavailableError.builder()
						.setText(text)
						.build();
			case 500:
				throw ServerError.builder()
						.setText(text)
						.build();
			default:
				throw ServerError.builder()
						.setText("Status code: " + status + ", text=" + text)
						.build();
		}
	}

	/** Parses a json rpc response. */
	@VisibleForTesting
	RpcResponse parseRpcResponse(final RestResponse response) {
		String content = response.getContent();
		return RpcResponse.parseJson(content);
	}

	/** Parses a successful rpc result. */
	@VisibleForTesting
	Object parseRpcResult(final Object result, final Invocation invocation) {
		DataDescriptor d = (DataDescriptor) invocation.getResult();
		return d.parseObject(result);
	}

	/** Parses an application exception. */
	@VisibleForTesting
	RuntimeException parseRpcException(final Object result, final Invocation invocation) {
		MessageDescriptor d = invocation.getExc();
		if (d == null) {
			throw ClientError.builder()
					.setText("Unsupported application exception: " + result)
					.build();
		}

		return (RuntimeException) d.parseObject(result);
	}

	/** Url-encodes a string. */
	private static String urlencode(final String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
