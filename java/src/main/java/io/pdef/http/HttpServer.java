package io.pdef.http;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.func.FluentFunction;
import io.pdef.rpc.RpcCall;
import io.pdef.rpc.RpcErrors;
import io.pdef.rpc.RpcRequest;
import io.pdef.rpc.RpcResponse;

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

	/** Parses an RPC request from an HTTP request. */
	public static RpcRequest readRequest(final HttpServletRequest request,
			final InterfaceDescriptor<?> descriptor) {
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
	public static void writeResponse(final HttpServletResponse response,
			final RpcResponse rpcResponse) {
		String json = rpcResponse.serializeToJson();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(json.length());
		response.setContentType(MediaType.JSON_UTF_8.toString());

		PrintWriter writer;
		try {
			writer = response.getWriter();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		writer.write(json);
		writer.flush();
	}

	/** Creates an HTTP request reader for an interface. */
	public static FluentFunction<HttpServletRequest, RpcRequest> requestReader(
			final InterfaceDescriptor<?> descriptor) {
		return new HttpRequestReader(descriptor);
	}

	/** Creates a response writer for a given HTTP response. */
	public static FluentFunction<RpcResponse, Void> responseWriter(
			final HttpServletResponse response) {
		return new HttpResponseWriter(response);
	}

	private static class HttpRequestReader extends FluentFunction<HttpServletRequest, RpcRequest> {
		private final InterfaceDescriptor<?> descriptor;

		private HttpRequestReader(final InterfaceDescriptor<?> descriptor) {
			this.descriptor = checkNotNull(descriptor);
		}

		@Override
		public RpcRequest apply(final HttpServletRequest input) {
			return readRequest(input, descriptor);
		}
	}

	private static class HttpResponseWriter extends FluentFunction<RpcResponse, Void> {
		private final HttpServletResponse response;

		private HttpResponseWriter(final HttpServletResponse response) {
			this.response = checkNotNull(response);
		}

		@Override
		public Void apply(final RpcResponse input) {
			writeResponse(response, input);
			return null;
		}
	}
}
