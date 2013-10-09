package io.pdef.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.pdef.types.TypeEnum;
import io.pdef.types.*;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationResult;
import io.pdef.rpc.*;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RestServerHandler implements Function<RestRequest, RestResponse> {
	private final InterfaceType type;
	private final Function<Invocation, InvocationResult> invoker;

	/** Creates a REST server handler. */
	public RestServerHandler(final Class<?> cls,
			final Function<Invocation, InvocationResult> invoker) {
		this.type = InterfaceType.findType(cls);
		this.invoker = checkNotNull(invoker);

		checkArgument(type != null, "Cannot find an interface type in %s", cls);
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
		InterfaceType type = this.type;
		while (!parts.isEmpty()) {
			String part = parts.removeFirst();

			// Find a method by name or get an index method.
			InterfaceMethod method = type.findMethod(part);
			if (method == null) method = type.getIndexMethod();
			if (method == null) {
				throw new MethodNotFoundError().setText("Method not found");
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
					throw new MethodNotAllowedError().setText("Method not allowed, POST required");
				}

				for (InterfaceMethodArg argd : method.getArgs()) {
					args.add(parseQueryArg(argd, post));
				}

			} else if (method.isRemote()) {
				// Parse arguments from the query string.
				for (InterfaceMethodArg argd : method.getArgs()) {
					args.add(parseQueryArg(argd, query));
				}

			} else {
				// Parse arguments as positional params.
				for (InterfaceMethodArg argd : method.getArgs()) {
					if (parts.isEmpty()) {
						throw new WrongMethodArgsError().setText("Wrong number of method args");
					}

					args.add(parsePositionalArg(argd, parts.removeFirst()));
				}
			}

			// Chain a method invocation with the parsed args.
			invocation = invocation.next(method, args.toArray());

			// Parse a next method or return an invocation chain.
			if (!method.isRemote()) {
				// Get the next interface and proceed parsing the parts.
				type = (InterfaceType) method.getResult();
				continue;
			}

			// It's a remote method. Stop parsing the parts.
			if (!parts.isEmpty()) {
				// Cannot have any more parts here, bad url.
				throw new MethodNotFoundError().setText("Method not found");
			}

			return invocation;
		}

		// The parts are empty.
		// No method invocations.
		throw new MethodNotFoundError().setText("Method not found");
	}

	@VisibleForTesting
	Object parsePositionalArg(final InterfaceMethodArg argd, final String s) {
		String value = Rest.urldecode(s);
		return parseArgFromString(argd.getType(), value);
	}

	@VisibleForTesting
	Object parseQueryArg(final InterfaceMethodArg argd, final Map<String, String> src) {
		DataType type = argd.getType();

		if ((type instanceof MessageType) && ((MessageType) type).isForm()) {
			// Parse as expanded form fields.

			MessageType messageType = (MessageType) type;
			Map<String, Object> fields = Maps.newHashMap();
			for (MessageField field : messageType.getFields()) {
				String fvalue = src.get(field.getName());
				if (fvalue == null) {
					continue;
				}

				Object parsed = parseArgFromString(field.getType(), fvalue);
				fields.put(field.getName(), parsed);
			}

			return messageType.parseNative(fields);
		}

		// Parse from a string.
		String value = src.get(argd.getName());
		if (value == null) {
			return null;
		}

		return parseArgFromString(type, value);
	}

	@VisibleForTesting
	Object parseArgFromString(final DataType type, final String value) {
		if (value == null) {
			return null;
		}

		if (value.equals("")) {
			return type.type() == TypeEnum.STRING ? "" : null;
		}

		return type.parseString(value);
	}

	@VisibleForTesting
	InvocationResult invoke(final Invocation invocation) throws Exception {
		return invoker.apply(invocation);
	}

	@VisibleForTesting
	RestResponse okResponse(final InvocationResult result, final Invocation invocation) {
		RpcResult rpc = new RpcResult();

		Object data = result.getData();
		if (result.isOk()) {
			// It's a successful method result.
			DataType d = (DataType) invocation.getResult();

			rpc.setStatus(RpcStatus.OK);
			rpc.setData(d.toNative(data));
		} else {
			// It's an expected application exception.
			DataType d = invocation.getExc();
			assert d != null;

			rpc.setStatus(RpcStatus.EXCEPTION);
			rpc.setData(d.toNative(data));
		}

		String content = rpc.toJson();
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
