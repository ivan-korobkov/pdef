package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import io.pdef.rpc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/** Server constructors and functions. */
public class Server {
	private Server() {}

	// === Http ===

	private static final Splitter HTTP_METHOD_SPLITTER = Splitter.on("/");

	public static interface HttpHandler {
		/** Handles an http request. */
		void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
	}

	/** Creates a default http handler from a descriptor and a service supplier. */
	public static <T> HttpHandler http(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		final Function<Request, Response> function = Server.rpc(descriptor, supplier);

		return new HttpHandler() {
			@Override
			public void handle(final HttpServletRequest request,
					final HttpServletResponse response) throws IOException {
				checkNotNull(request);
				checkNotNull(response);

				Response resp;
				try {
					Request req = httpParseResponse(descriptor, request);
					resp = function.apply(req);
				} catch (Exception e) {
					resp = Server.rpcSerializeError(e);
				}

				httpWriteResponse(response, resp);
			}
		};
	}

	/** Parses an rpc request from an http request. */
	public static Request httpParseResponse(final InterfaceDescriptor<?> descriptor,
			final HttpServletRequest request) {
		checkNotNull(request);
		String pathInfo = request.getPathInfo();
		pathInfo = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Iterator<String> result = HTTP_METHOD_SPLITTER.split(pathInfo).iterator();

		List<MethodCall> calls = Lists.newArrayList();
		while (result.hasNext()) {
			String name = result.next();
			path.append(path.length() == 0 ? "" : "/").append(name);
			if (d == null) throw RpcErrors.methodNotFound(path);
			MethodDescriptor method = d.getMethod(name);
			if (method == null) throw RpcErrors.methodNotFound(path);

			// Consume the arguments.
			Map<String, Object> args = Maps.newLinkedHashMap();
			for (String key : method.getArgs().keySet()) {
				if (!result.hasNext()) throw RpcErrors.wrongMethodArgs(path);
				String arg = result.next();
				args.put(key, arg);
			}

			MethodCall call = MethodCall.builder()
					.setMethod(name)
					.setArgs(args)
					.build();
			calls.add(call);
			if (!method.isRemote()) d = method.getNext();
		}

		return Request.builder()
				.setCalls(calls)
				.build();
	}

	/** Writes an rpc response to an http response. */
	public static void httpWriteResponse(final HttpServletResponse response, final Response resp)
			throws IOException {
		String json = resp.serializeToJson();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(json.length());
		response.setContentType(MediaType.JSON_UTF_8.toString());
		PrintWriter writer = response.getWriter();
		writer.write(json);
		writer.flush();
	}


	// === RPC ==

	/** Creates an rpc handler from a descriptor and a service supplier. */
	public static <T> Function<Request, Response> rpc(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		checkNotNull(descriptor);
		checkNotNull(supplier);
		return new Function<Request, Response>() {
			@Override
			public Response apply(final Request request) {
				return rcpHandleRequest(request, descriptor, supplier);
			}
		};
	}

	/** Handles an rpc request and returns an rpc response, never throws exceptions. */
	public static <T> Response rcpHandleRequest(final Request request,
			final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		try {
			if (request == null) throw RpcErrors.badRequest();

			Object result;
			Invocation invocation = rpcParseRequest(descriptor, request);
			try {
				Object service = supplier.get();
				result = invocation.invokeChainOn(service);
			} catch (Exception e) {
				return rpcSerializeExcOrPropagate(invocation, e);
			}

			return rpcSerializeResult(invocation, result);
		} catch (Exception e) {
			return rpcSerializeError(e);
		}
	}

	/** Parses a request into an proxy chain. */
	public static <T> Invocation rpcParseRequest(final InterfaceDescriptor<T> descriptor,
			final Request request) {
		checkNotNull(request);
		List<MethodCall> calls = request.getCalls();
		if (calls.isEmpty()) throw RpcErrors.methodCallsRequired();

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Invocation invocation = Invocation.root();

		for (final MethodCall call : calls) {
			String name = call.getMethod();
			path.append(path.length() == 0 ? "" : ".").append(name);
			if (d == null) throw RpcErrors.methodNotFound(path);

			MethodDescriptor method = d.getMethods().get(name);
			if (method == null) throw RpcErrors.methodNotFound(path);

			try {
				invocation = method.parse(invocation, call.getArgs());
			} catch (Exception e) {
				throw RpcErrors.wrongMethodArgs(path);
			}

			if (!invocation.isRemote()) d = invocation.getNext();
		}

		if (!invocation.isRemote()) throw RpcErrors.notRemoteMethod(path);
		return invocation;
	}

	/** Serializes a remote proxy result. */
	@SuppressWarnings("unchecked")
	public static Response rpcSerializeResult(final Invocation remote, final Object result) {
		Descriptor resultDescriptor = remote.getResult();
		Object response = resultDescriptor.serialize(result);
		return Response.builder()
				.setResult(response)
				.setStatus(ResponseStatus.OK)
				.build();
	}

	/** Serializes a remote proxy exception or propagates the exception. */
	@SuppressWarnings("unchecked")
	public static Response rpcSerializeExcOrPropagate(final Invocation invocation,
			final Exception e) {
		Descriptor excDescriptor = invocation.getExc();
		if (excDescriptor == null || !excDescriptor.getJavaClass().isInstance(e)) {
			throw Throwables.propagate(e);
		}

		// It's an application exception.
		Object result = excDescriptor.serialize(e);
		return Response.builder()
				.setStatus(ResponseStatus.EXCEPTION)
				.setResult(result)
				.build();
	}

	/** Serializes an internal server error. */
	public static Response rpcSerializeError(final Exception e) {
		RpcError error = e instanceof RpcError ? (RpcError) e : RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
		Object result = error.serialize();
		return Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(result)
				.build();
	}
}
