package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

public class HttpServerHandler extends HttpServlet {
	private static final Splitter SLASH_SPLITTER = Splitter.on("/");
	private final Function<Request, Response> handler;

	public HttpServerHandler(final Function<Request, Response> handler) {
		this.handler = handler;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		handle(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		handle(req, resp);
	}

	private void handle(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		Request request = read(req);
		Response response = handler.apply(request);
		write(response, resp);
	}

	public Request read(final HttpServletRequest req) {
		String s = req.getPathInfo();
		if (s.startsWith("/")) s = s.substring(1);

		StringBuilder path = new StringBuilder();
		Iterator<String> iterator = SLASH_SPLITTER.split(s).iterator();
		if (!iterator.hasNext()) throw Errors.methodCallsRequired();

		ImmutableList.Builder<MethodCall> b = ImmutableList.builder();
//		while (iterator.hasNext()) {
//			String name = iterator.next();
//			path.append("/");
//			path.append(name);
//
//			//PdefMethod method = descriptor.getMethod(name);
//			if (method == null) throw methodNotFound(path);
//
//			ImmutableMap.Builder<String, Object> ab = ImmutableMap.builder();
//			for (Map.Entry<String, PdefDatatype> entry : method.getArgs().entrySet()) {
//				if (!iterator.hasNext()) {
//					throw wrongNumberOfMethodArgs(path, method.getArgNum(), ab.build().size());
//				}
//
//				Object arg;
//				try {
//					arg = format.read(entry.getValue(), iterator.next());
//				} catch (FormatException e) {
//					throw wrongMethodArgs(path);
//				}
//				ab.put(entry.getKey(), arg);
//			}
//
//			MethodCall call = MethodCall.builder()
//					.setMethod(name)
//					.setArgs(ab.build())
//					.build();
//			b.add(call);
//
//			if (method.isInterface()) {
//				// The method returns and interface, there should be more calls.
//				if (!iterator.hasNext()) throw dataMethodCallRequired(path);
//
//				descriptor = (PdefInterface) method.getResult();
//				continue;
//			}
//
//			// It must be the last data type method.
//			if (iterator.hasNext()) throw dataMethodReachedNoMoCalls(path);
//		}

		return Request.builder()
				.setCalls(b.build())
				.build();
	}

	public void write(final Response response, final HttpServletResponse httpResponse)
			throws IOException {
		String result = ""; //format.write(response);

		int httpStatus = HttpServletResponse.SC_OK;
		ResponseStatus status = response.getStatus();
		switch (status != null ? status : ResponseStatus.ERROR) {
			case OK:
			case EXCEPTION:
				httpStatus = HttpServletResponse.SC_OK;
				break;
			case ERROR:
				io.pdef.rpc.Error e = (io.pdef.rpc.Error) response.getResult();
				ErrorCode code = e.getCode();
				switch (code != null ? code : ErrorCode.SERVER_ERROR) {
					case SERVER_ERROR:
						httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
						break;
					case BAD_REQUEST:
						httpStatus = HttpServletResponse.SC_BAD_REQUEST;
						break;
					case SERVICE_UNAVAILABLE:
						httpStatus = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
						break;
					case TIMEOUT:
						httpStatus = HttpServletResponse.SC_REQUEST_TIMEOUT;
						break;
				}
		}

		httpResponse.setStatus(httpStatus);
		httpResponse.setContentType(MediaType.JSON_UTF_8.toString());
		httpResponse.getWriter().write(result);
	}
}
