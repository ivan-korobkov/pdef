package io.pdef;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;
import io.pdef.rpc.RpcErrors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpServer<T> {
	private static final Splitter METHOD_SPLITTER = Splitter.on("/");
	private final InterfaceDescriptor<T> descriptor;
	private final Server<T> server;

	protected HttpServer(final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		this.descriptor = checkNotNull(descriptor);
		server = Server.create(descriptor, supplier);
	}

	public static <T> HttpServer<T> create(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		return new HttpServer<T>(descriptor, supplier);
	}

	public void handle(final HttpServletRequest request, final HttpServletResponse response) {
		checkNotNull(request);
		checkNotNull(response);

		Response resp;
		try {
			String path = request.getPathInfo();
			Request req = parseRequest(descriptor, request);
			resp = server.apply(req);
		} catch (Exception e) {
			resp = server.serializeError(e);
		}

		writeResponse(response, resp);
	}

	public static Request parseRequest(final InterfaceDescriptor<?> descriptor,
			final HttpServletRequest request) {
		checkNotNull(request);
		String pathInfo = request.getPathInfo();

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

	public static void writeResponse(final HttpServletResponse response, final Response resp) {

	}
}
