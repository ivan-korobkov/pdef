package pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import pdef.Invocation;
import pdef.TypeEnum;
import pdef.descriptors.*;
import pdef.rpc.*;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RestServer<T> implements Function<RestRequest, RestResponse> {
	public static final String CHARSET = "UTF-8";
	private final InterfaceDescriptor descriptor;
	private final Supplier<T> serviceSupplier;

	public RestServer(final Class<T> cls, final Supplier<T> serviceSupplier) {
		this.descriptor = InterfaceDescriptor.findDescriptor(cls);
		this.serviceSupplier = checkNotNull(serviceSupplier);

		checkArgument(descriptor != null, "Cannot find an interface descriptor in %s", cls);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		return handle(request);
	}

	public RestResponse handle(final RestRequest request) {
		checkNotNull(request);

		try {
			RpcResponse response;
			Invocation invocation = parseRequest(request);
			try {
				Object result = invoke(invocation);
				response = resultToResponse(result, invocation);
			} catch (Exception e) {
				response = handleException(e, invocation);

				if (response == null) {
					return errorRestResponse(e);
				}
			}

			return restResponse(response);
		} catch (Exception e) {
			return errorRestResponse(e);
		}
	}

	private Invocation parseRequest(final RestRequest request) throws Exception {
		checkNotNull(request);
		String path = request.getPath();
		Map<String, String> query = request.getQuery();
		Map<String, String> post = request.getPost();

		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		LinkedList<String> parts = Lists.newLinkedList();
		Collections.addAll(parts, path.split("/"));

		Invocation invocation = Invocation.root();
		InterfaceDescriptor descriptor = this.descriptor;
		while (!parts.isEmpty()) {
			String part = parts.removeFirst();

			// Find a method by name or get an index method.
			if (descriptor == null) {
				throw MethodNotFoundError.builder()
						.setText("Method not found")
						.build();
			}

			MethodDescriptor method = descriptor.findMethod(part);
			if (method == null) method = descriptor.getIndexMethod();
			if (method == null) {
				throw MethodNotFoundError.builder()
						.setText("Method not found")
						.build();
			}

			// If an index method, prepend the part back,
			// because index methods do not have names.
			if (method.isIndex() && !part.equals("")) {
				parts.addFirst(part);
			}

			// Parse method arguments.
			List<Object> args = Lists.newArrayList();
			if (method.isPost()) {
				// Parse arguments from the post body.
				if (!request.isPost()) {
					throw MethodNotAllowedError.builder()
							.setText("Method not allowed, POST required")
							.build();
				}

				for (ArgDescriptor argd : method.getArgs()) {
					args.add(parseQueryArg(argd, post));
				}

			} else if (method.isRemote()) {
				// Parse arguments from the query string.
				for (ArgDescriptor argd : method.getArgs()) {
					args.add(parseQueryArg(argd, query));
				}

			} else {
				// Parse arguments as positional params.
				for (ArgDescriptor argd : method.getArgs()) {
					if (parts.isEmpty()) {
						throw WrongMethodArgsError.builder()
								.setText("Wrong number of method args")
								.build();
					}

					args.add(parsePositionalArg(argd, parts.removeFirst()));
				}
			}

			invocation = invocation.next(method, args.toArray());
			descriptor = method.isRemote() ? null : (InterfaceDescriptor) method.getResult();
		}

		if (!invocation.isRemote()) {
			throw MethodNotFoundError.builder()
					.setText("Method not found")
					.build();
		}

		return invocation;
	}

	private Object parsePositionalArg(final ArgDescriptor argd, final String s)
			throws UnsupportedEncodingException {
		String value = URLDecoder.decode(s, CHARSET);
		return parseArgFromString(argd.getType(), value);
	}

	private Object parseQueryArg(final ArgDescriptor argd, final Map<String, String> src) {
		DataDescriptor d = argd.getType();
		MessageDescriptor md = d.asMessageDescriptor();
		boolean isForm = md != null && md.isForm();

		if (!isForm) {
			// Parse as a string argument.
			String value = src.get(argd.getName());
			if (value == null) {
				return null;
			}

			return parseArgFromString(d, value);
		}

		// Parse as expanded form fields.
		Map<String, Object> fields = Maps.newHashMap();
		for (FieldDescriptor fd : md.getFields()) {
			String fvalue = src.get(fd.getName());
			if (fvalue == null) {
				continue;
			}

			fields.put(fd.getName(), parseArgFromString(fd.getType(), fvalue));
		}

		return md.parseObject(fields);
	}

	private Object parseArgFromString(final DataDescriptor descriptor, final String value) {
		TypeEnum type = descriptor.getType();

		if (value == null) {
			return null;
		}

		if (value.equals("")) {
			return type == TypeEnum.STRING ? "" : null;
		}

		if (type.isPrimitive()) {
			return ((PrimitiveDescriptor) descriptor).parseString(value);
		}

		return descriptor.parseJson(value);
	}

	@VisibleForTesting
	Object invoke(final Invocation invocation) throws Exception {
		Object service = serviceSupplier.get();
		return invocation.invoke(service);
	}

	@VisibleForTesting
	RpcResponse resultToResponse(final Object result, final Invocation invocation) {
		DataDescriptor dd = (DataDescriptor) invocation.getResult();
		Object serialized = dd.toObject(result);

		return RpcResponse.builder()
				.setStatus(RpcStatus.OK)
				.setResult(serialized)
				.build();
	}

	@VisibleForTesting
	RpcResponse handleException(final Exception e, final Invocation invocation) {
		MessageDescriptor excd = invocation.getExc();
		if (excd == null || !excd.getJavaClass().isInstance(e)) {
			return null;
		}

		Object serialized = excd.toObject(e);
		return RpcResponse.builder()
				.setStatus(RpcStatus.EXCEPTION)
				.setResult(serialized)
				.build();
	}

	@VisibleForTesting
	RestResponse restResponse(final RpcResponse response) {
		String json = response.toJson();

		return new RestResponse()
				.withOkStatus()
				.withJsonContentType()
				.setContent(json);
	}

	@VisibleForTesting
	RestResponse errorRestResponse(final Exception e) {
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
				.withTextContentType();
	}
}
