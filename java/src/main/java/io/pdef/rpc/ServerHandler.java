package io.pdef.rpc;

import com.google.common.base.Function;
import io.pdef.fluent.FluentFunctions;
import io.pdef.invocation.RemoteInvocation;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerHandler implements Function<Request, Response> {
	private final Function<Request, Response> delegate;

	public ServerHandler(final Class<?> iface, final Object service) {
		delegate = FluentFunctions
				.of(new RequestToInvocation(iface))
				.then(new InvocationToResult(service))
				.then(new ResultToResponse());
	}

	@Override
	public Response apply(final Request input) {
		return delegate.apply(input);
	}

	/** Reads a remote invocation from a request. */
	public static class RequestToInvocation implements Function<Request, RemoteInvocation> {
		private final Class<?> iface;

		public RequestToInvocation(final Class<?> iface) {
			this.iface = checkNotNull(iface);
		}

		@Override
		public RemoteInvocation apply(final Request input) {
			checkNotNull(input);
			return null;
		}
	}

	/** Handles a remote invocation and returns the result. */
	public static class InvocationToResult implements Function<RemoteInvocation, Object> {
		private final Object service;

		public InvocationToResult(final Object service) {
			this.service = checkNotNull(service);
		}

		@Override
		public Object apply(final RemoteInvocation input) {
			return null;
		}
	}

	public static class ResultToResponse implements Function<Object, Response> {
		@Override
		public Response apply(final Object input) {
			return null;
		}
	}

	public static class ExceptionToResponse implements Function<Exception, Response> {
		@Override
		public Response apply(final Exception input) {
			return null;
		}
	}
}
