package io.pdef.rest;

import io.pdef.Func;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

class HttpSession implements Func<Request, Response> {
	@Override
	public Response apply(final Request request) throws Exception {
		return request.execute();
	}
}
