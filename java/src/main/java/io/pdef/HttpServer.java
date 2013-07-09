package io.pdef;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class HttpServer {
	private static final Splitter METHOD_SPLITTER = Splitter.on("/");

	/** Handles an http request. */
	public abstract void handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException;

	/** Creates a default http handler from a descriptor and a service supplier. */
	public static <T> HttpServer create(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		final Function<Request, Response> function = Server.create(descriptor, supplier);

		return new HttpServer() {
			@Override
			public void handle(final HttpServletRequest request,
					final HttpServletResponse response) throws IOException {
				checkNotNull(request);
				checkNotNull(response);

				Response resp;
				try {
					Request req = parseRequest(descriptor, request);
					resp = function.apply(req);
				} catch (Exception e) {
					resp = Server.serializeError(e);
				}

				writeResponse(response, resp);
			}
		};
	}

	/** Parses an rpc request from an http request. */
	public static Request parseRequest(final InterfaceDescriptor<?> descriptor,
			final HttpServletRequest request) {
		checkNotNull(request);
		String pathInfo = request.getPathInfo();
		pathInfo = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Iterator<String> result = METHOD_SPLITTER.split(pathInfo).iterator();

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
	public static void writeResponse(final HttpServletResponse response, final Response resp)
			throws IOException {
		String json = resp.serializeToJson();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(json.length());
		response.setContentType(MediaType.JSON_UTF_8.toString());
		PrintWriter writer = response.getWriter();
		writer.write(json);
		writer.flush();
	}
}
