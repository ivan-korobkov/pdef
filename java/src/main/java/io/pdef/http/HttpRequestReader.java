package io.pdef.http;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.*;
import io.pdef.formats.FormatException;
import io.pdef.formats.StringFormat;
import io.pdef.rpc.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.pdef.rpc.RpcExceptions.*;

public class HttpRequestReader {
	private static final Splitter SPLITTER = Splitter.on("/");
	private final StringFormat format;
	private final PdefInterface iface;

	public HttpRequestReader(final Class<?> cls, final Pdef pdef) {
		format = new StringFormat(pdef);
		iface = (PdefInterface) pdef.get(cls);
	}

	public Request read(final HttpServletRequest request) throws RpcException {
		return read(request.getPathInfo());
	}

	public Request read(final String requestPath) {
		checkNotNull(requestPath);
		String s = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
		PdefInterface descriptor = iface;
		StringBuilder path = new StringBuilder();

		Iterator<String> iterator = SPLITTER.split(s).iterator();
		if (!iterator.hasNext()) throw RpcExceptions.methodCallsRequired();

		ImmutableList.Builder<MethodCall> b = ImmutableList.builder();
		while (iterator.hasNext()) {
			String name = iterator.next();
			path.append("/");
			path.append(name);

			PdefMethod method = descriptor.getMethods().get(name);
			if (method == null) throw methodNotFound(path);

			ImmutableMap.Builder<String, Object> ab = ImmutableMap.builder();
			for (Map.Entry<String, PdefDatatype> entry : method.getArgs().entrySet()) {
				if (!iterator.hasNext()) throw wrongNumberOfMethodArgs(path, method);

				Object arg;
				try {
					arg = format.read(entry.getValue(), iterator.next());
				} catch (FormatException e) {
					throw wrongMethodArgs(path);
				}
				ab.put(entry.getKey(), arg);
			}

			MethodCall call = MethodCall.builder()
					.setMethod(name)
					.setArgs(ab.build())
					.build();
			b.add(call);

			if (method.isInterface()) {
				// The method returns and interface, there should be more calls.
				if (!iterator.hasNext()) throw moreMethodCallsRequired(path);

				descriptor = (PdefInterface) method.getResult();
				continue;
			}

			// It must be the last data type method.
			if (iterator.hasNext()) throw methodCallNotSupported(path);
		}

		return Request.builder()
				.setCalls(b.build())
				.build();
	}
}
