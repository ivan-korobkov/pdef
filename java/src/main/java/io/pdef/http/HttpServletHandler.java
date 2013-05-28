package io.pdef.http;

import io.pdef.Pdef;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpServletHandler<T> extends HttpServlet {
	private final HttpHandler<T> handler;

	public HttpServletHandler(final T service, final Class<T> cls, final Pdef pdef) {
		this.handler = new HttpHandler<T>(service, cls, pdef);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		handler.handle(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		handler.handle(req, resp);
	}
}
