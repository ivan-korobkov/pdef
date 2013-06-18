package io.pdef.server;

import com.google.common.base.Function;
import io.pdef.rpc.Response;

public class ExceptionToResponse implements Function<Exception, Response> {
	@Override
	public Response apply(final Exception input) {
		return null;
	}
}
