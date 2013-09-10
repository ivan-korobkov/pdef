package pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import pdef.Invocation;
import pdef.InvocationResult;
import pdef.TypeEnum;
import pdef.descriptors.*;
import pdef.rpc.*;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RestServerHandler implements Function<RestRequest, RestResponse> {
	private final InterfaceDescriptor descriptor;
	private final Function<Invocation, InvocationResult> invoker;

	/** Creates a REST server handler. */
	public RestServerHandler(final Class<?> cls,
			final Function<Invocation, InvocationResult> invoker) {
		this.descriptor = InterfaceDescriptor.findDescriptor(cls);
		this.invoker = checkNotNull(invoker);

		checkArgument(descriptor != null, "Cannot find an interface descriptor in %s", cls);
	}

	@Override
	public RestResponse apply(final RestRequest request) {
		return handle(request);
	}

	public RestResponse handle(final RestRequest request) {
		checkNotNull(request);

		try {
			Invocation invocation = parseRequest(request);
			InvocationResult result = invoke(invocation);
			return okResponse(result, invocation);
		} catch (Exception e) {
			// Catch any unhandled (not application-specific) exception and
			// convert it into a server error response.
			return errorResponse(e);
		}
	}

	@VisibleForTesting
	Invocation parseRequest(final RestRequest request) throws Exception {
		checkNotNull(request);
		String path = request.getPath();
		Map<String, String> query = request.getQuery();
		Map<String, String> post = request.getPost();

		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// Split the path into method names and positions arguments parts.
		String[] partsArray = path.split("/", -1); // -1 disables discarding trailing empty strings.
		LinkedList<String> parts = Lists.newLinkedList();
		Collections.addAll(parts, partsArray);


		// Parse the parts as method invocations.
		Invocation invocation = Invocation.root();
		InterfaceDescriptor descriptor = this.descriptor;
		while (!parts.isEmpty()) {
			String part = parts.removeFirst();

			// Find a method by name or get an index method.
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

			// Chain a method invocation with the parsed args.
			invocation = invocation.next(method, args.toArray());

			// Parse a next method or return an invocation chain.
			if (!method.isRemote()) {
				// Get the next interface and proceed parsing the parts.
				descriptor = (InterfaceDescriptor) method.getResult();
				continue;
			}

			// It's a remote method. Stop parsing the parts.
			if (!parts.isEmpty()) {
				// Cannot have any more parts here, bad url.
				throw MethodNotFoundError.builder()
						.setText("Method not found")
						.build();
			}

			return invocation;
		}

		// The parts are empty.
		// No method invocations.
		throw MethodNotFoundError.builder()
				.setText("Method not found")
				.build();
	}

	@VisibleForTesting
	Object parsePositionalArg(final ArgDescriptor argd, final String s) {
		String value = Rest.urldecode(s);
		return parseArgFromString(argd.getType(), value);
	}

	@VisibleForTesting
	Object parseQueryArg(final ArgDescriptor argd, final Map<String, String> src) {
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

	@VisibleForTesting
	Object parseArgFromString(final DataDescriptor descriptor, final String value) {
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
	InvocationResult invoke(final Invocation invocation) throws Exception {
		return invoker.apply(invocation);
	}

	@VisibleForTesting
	RestResponse okResponse(final InvocationResult result, final Invocation invocation) {
		RpcResult.Builder rpc = RpcResult.builder();

		Object data = result.getData();
		if (result.isOk()) {
			// It's a successful method result.
			DataDescriptor d = (DataDescriptor) invocation.getResult();

			rpc.setStatus(RpcStatus.OK);
			rpc.setData(d.toObject(data));
		} else {
			// It's an expected application exception.
			DataDescriptor d = invocation.getExc();
			assert d != null;

			rpc.setStatus(RpcStatus.EXCEPTION);
			rpc.setData(d.toObject(data));
		}

		String content = rpc.build().toJson();
		return new RestResponse()
				.setOkStatus()
				.setContent(content)
				.setJsonContentType();
	}

	@VisibleForTesting
	RestResponse errorResponse(final Exception e) {
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
