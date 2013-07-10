package io.pdef;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;
import io.pdef.rpc.RpcErrors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerHttpProtocol<T> implements Function<ServerHttpProtocol.RequestResponse, Void> {
	private static final Splitter HTTP_METHOD_SPLITTER = Splitter.on("/");
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Request, Response> requestHandler;

	private ServerHttpProtocol(final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> requestHandler) {
		this.descriptor = checkNotNull(descriptor);
		this.requestHandler = checkNotNull(requestHandler);
	}

	/** Creates a simple http rpc server as httpFilter.then(rpcFilter).then(create). */
	public static <T> Function<RequestResponse, Void> server(
			final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		return filter(descriptor)
				.then(ServerRpcProtocol.filter(descriptor))
				.then(ServerInvocationHandler.create(supplier));
	}

	/** Creates an http handler. */
	public static <T> Function<RequestResponse, Void> handler(
			final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> requestHandler) {
		return new ServerHttpProtocol<T>(descriptor, requestHandler);
	}

	/** Creates an http filter. */
	public static <T> Filter<RequestResponse, Void, Request, Response> filter(
			final InterfaceDescriptor<T> descriptor) {
		return new HttpFilter<T>(descriptor);
	}

	@Override
	public Void apply(@Nullable final RequestResponse requestResponse) {
		handle(requestResponse, descriptor, requestHandler);
		return null;
	}

	/** Handles an http request and writes a result to an http response, returns null.
	 * @throws RuntimeException if fails to write to an http response. */
	@VisibleForTesting
	  static void handle(final RequestResponse requestResponse,
			final InterfaceDescriptor<?> descriptor,
			final Function<Request, Response> requestHandler) {
		HttpServletRequest request = requestResponse.getRequest();
		HttpServletResponse response = requestResponse.getResponse();

		Response resp;
		try {
			Request req = httpParseResponse(descriptor, request);
			resp = requestHandler.apply(req);
		} catch (Exception e) {
			resp = ServerRpcProtocol.serializeError(e);
		}

		try {
			httpWriteResponse(response, resp);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Parses an rpc request from an http request. */
	@VisibleForTesting
	static Request httpParseResponse(final InterfaceDescriptor<?> descriptor,
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
	@VisibleForTesting
	static void httpWriteResponse(final HttpServletResponse response, final Response resp)
			throws IOException {
		String json = resp.serializeToJson();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(json.length());
		response.setContentType(MediaType.JSON_UTF_8.toString());
		PrintWriter writer = response.getWriter();
		writer.write(json);
		writer.flush();
	}

	/** Combines an http request and a response into one function arg. */
	public static class RequestResponse {
		private final HttpServletRequest request;
		private final HttpServletResponse response;

		public RequestResponse(final HttpServletRequest request,
				final HttpServletResponse response) {
			this.request = checkNotNull(request);
			this.response = checkNotNull(response);
		}

		public HttpServletRequest getRequest() {
			return request;
		}

		public HttpServletResponse getResponse() {
			return response;
		}
	}

	private static class HttpFilter<T>
			extends AbstractFilter<RequestResponse, Void, Request, Response> {
		private final InterfaceDescriptor<T> descriptor;

		private HttpFilter(final InterfaceDescriptor<T> descriptor) {
			this.descriptor = checkNotNull(descriptor);
		}

		@Override
		public Void apply(final RequestResponse input, final Function<Request,
				Response> next) {
			handle(input, descriptor, next);
			return null;
		}
	}
}
