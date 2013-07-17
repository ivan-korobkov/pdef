package io.pdef.http;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.func.FluentFilter;
import io.pdef.rpc.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpServer {
	private static final Splitter HTTP_METHOD_SPLITTER = Splitter.on("/");
	private HttpServer() {}

	/** Handles an http request and writes a result to an http response, returns null.
	 * @throws RuntimeException if fails to write to an http response. */
	public static void apply(final HttpRequestResponse requestResponse,
			final InterfaceDescriptor<?> descriptor,
			final Function<RpcRequest, RpcResponse> rpcHandler) {
		HttpServletRequest request = requestResponse.getRequest();
		HttpServletResponse response = requestResponse.getResponse();

		RpcResponse resp;
		try {
			RpcRequest req = parse(descriptor, request);
			resp = rpcHandler.apply(req);
		} catch (Exception e) {
			resp = RpcResponses.error(e);
		}

		try {
			write(response, resp);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Parses an rpc request from an http request. */
	@VisibleForTesting
	static RpcRequest parse(final InterfaceDescriptor<?> descriptor,
			final HttpServletRequest request) {
		checkNotNull(request);
		String pathInfo = request.getPathInfo();
		pathInfo = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Iterator<String> result = HTTP_METHOD_SPLITTER.split(pathInfo).iterator();

		List<RpcCall> calls = Lists.newArrayList();
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

			RpcCall call = RpcCall.builder()
					.setMethod(name)
					.setArgs(args)
					.build();
			calls.add(call);
			if (!method.isRemote()) d = method.getNext();
		}

		Map<String, Object> meta = Maps.newHashMap();
		Enumeration<String> headers = request.getHeaderNames();
		if (headers != null) {
			while (headers.hasMoreElements()) {
				String key = headers.nextElement();
				String value = request.getHeader(key);
				meta.put(key, value);
			}
		}

		return RpcRequest.builder()
				.setCalls(calls)
				.setMeta(meta)
				.build();
	}

	/** Writes an rpc response to an http response. */
	@VisibleForTesting
	static void write(final HttpServletResponse response, final RpcResponse resp)
			throws IOException {
		String json = resp.serializeToJson();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(json.length());
		response.setContentType(MediaType.JSON_UTF_8.toString());
		PrintWriter writer = response.getWriter();
		writer.write(json);
		writer.flush();
	}


	/** Creates a simple http rpc server. */
	public static <T> Function<HttpRequestResponse, Void> create(
			final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		return filter(descriptor)
				.then(RpcServer.filter(descriptor))
				.then(RpcInvoker.from(supplier));
	}

	/** Creates an http filter. */
	public static <T> FluentFilter<HttpRequestResponse, Void, RpcRequest, RpcResponse> filter(
			final InterfaceDescriptor<T> descriptor) {
		checkNotNull(descriptor);

		return new FluentFilter<HttpRequestResponse, Void, RpcRequest, RpcResponse>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(HttpServer.class)
						.addValue(this)
						.toString();
			}

			@Override
			public Void apply(final HttpRequestResponse input,
					final Function<RpcRequest, RpcResponse> next) {
				HttpServer.apply(input, descriptor, next);
				return null;
			}
		};
	}

	/** Creates an http handler. */
	public static <T> Function<HttpRequestResponse, Void> function(
			final InterfaceDescriptor<T> descriptor,
			final Function<RpcRequest, RpcResponse> rpcHandler) {
		checkNotNull(descriptor);
		checkNotNull(rpcHandler);

		return new Function<HttpRequestResponse, Void>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(HttpServer.class)
						.addValue(this)
						.toString();
			}

			@Override
			public Void apply(final HttpRequestResponse input) {
				HttpServer.apply(input, descriptor, rpcHandler);
				return null;
			}
		};
	}
}
