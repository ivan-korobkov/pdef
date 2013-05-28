package io.pdef.http;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.FormatException;
import io.pdef.Pdef;
import io.pdef.StringFormat;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.Request;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptionCode;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpRequestReader {
	private static final Splitter SPLITTER = Splitter.on("/");
	private final StringFormat format;
	private final Pdef.InterfaceInfo info;

	public HttpRequestReader(final Class<?> cls, final Pdef pdef) {
		format = new StringFormat(pdef);
		info = (Pdef.InterfaceInfo) pdef.get(cls);
	}

	public Request read(final HttpServletRequest request) throws RpcException {
		return read(request.getPathInfo());
	}

	public Request read(final String requestPath) {
		checkNotNull(requestPath);

		String s = requestPath;
		if (s.startsWith("/")) s = s.substring(1);
		Iterable<String> parts = SPLITTER.split(s);
		Iterator<String> iterator = parts.iterator();

		StringBuilder path = new StringBuilder();
		Pdef.InterfaceInfo iface = info;

		ImmutableList.Builder<MethodCall> callBuilder = ImmutableList.builder();
		while (iterator.hasNext()) {
			String methodName = iterator.next();
			path.append("/");
			path.append(methodName);

			Pdef.MethodInfo methodInfo = iface == null ? null : iface.getMethods().get(methodName);
			if (methodInfo == null) {
				throw RpcException.builder()
						.setCode(RpcExceptionCode.BAD_REQUEST)
						.setText("Method not found: " + path)
						.build();
			}

			ImmutableMap.Builder<String, Object> argBuilder = ImmutableMap.builder();
			for (Map.Entry<String, Pdef.TypeInfo> entry : methodInfo.getArgs().entrySet()) {
				if (!iterator.hasNext()) {
					throw RpcException.builder()
							.setCode(RpcExceptionCode.BAD_REQUEST)
							.setText("Wrong number of arguments: " + path)
							.build();
				}

				Object arg;
				try {
					arg = format.read(entry.getValue(), iterator.next());
				} catch (FormatException e) {
					throw RpcException.builder()
							.setCode(RpcExceptionCode.BAD_REQUEST)
							.setText("Failed to parse arguments: " + path)
							.build();
				}
				argBuilder.put(entry.getKey(), arg);
			}

			MethodCall call = MethodCall.builder()
					.setMethod(methodName)
					.setArgs(argBuilder.build())
					.build();
			callBuilder.add(call);

			Pdef.TypeInfo resultInfo = methodInfo.getResult();
			if (resultInfo.getType() == Pdef.TypeEnum.INTERFACE) {
				iface = (Pdef.InterfaceInfo) resultInfo;
			} else {
				iface = null;
			}
		}

		return Request.builder()
				.setCalls(callBuilder.build())
				.build();
	}
}
