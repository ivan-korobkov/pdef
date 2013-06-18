package io.pdef.server;

import com.google.common.base.Function;
import io.pdef.fluent.FluentFunctions;
import io.pdef.invocation.RemoteInvocation;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;

public class ServerBuilder<T> {
	private Function<Request, RemoteInvocation> requestToInvocation;
	private Function<RemoteInvocation, Object> invocationToResult;
	private Function<Object, Response> resultToResponse;
	private Function<Exception, Response> exceptionToResponse;

	private ServerBuilder(final Class<T> iface, final T service) {
		requestToInvocation = new RequestToInvocation(iface);
		invocationToResult = new InvocationToResult(service);
		resultToResponse = new ResultToResponse();
		exceptionToResponse = new ExceptionToResponse();
	}

	public static <T> ServerBuilder<T> create(final Class<T> iface, final T service) {
		return new ServerBuilder<T>(iface, service);
	}

	public Function<Request, Response> build() {
		return FluentFunctions.of(requestToInvocation)
				.then(invocationToResult)
				.then(resultToResponse)
				.onError(exceptionToResponse);
	}
}
