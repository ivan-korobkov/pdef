package io.pdef;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

class ServerRpcProtocol<T> implements Function<Request, Response> {
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Invocation, Object> invocationHandler;

	ServerRpcProtocol(final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> invocationHandler) {
		this.descriptor = checkNotNull(descriptor);
		this.invocationHandler = checkNotNull(invocationHandler);
	}

	@Override
	public Response apply(final Request request) {
		return handle(request, descriptor, invocationHandler);
	}

	/** Handles an rpc request and returns an rpc response, never throws exceptions. */
	@VisibleForTesting
	static <T> Response handle(final Request request, final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> invocationHandler) {
		try {
			if (request == null) throw RpcErrors.badRequest();

			Object result;
			Invocation invocation = parseRequest(descriptor, request);
			try {
				result = invocationHandler.apply(invocation);
			} catch (Exception e) {
				return exceptionResponseOrPropagate(invocation, e);
			}

			return serializeResult(invocation, result);
		} catch (Exception e) {
			return errorResponse(e);
		}
	}

	/** Parses a request into an proxy chain. */
	@VisibleForTesting
	static Invocation parseRequest(final InterfaceDescriptor<?> descriptor,
			final Request request) {
		checkNotNull(request);
		List<MethodCall> calls = request.getCalls();
		if (calls.isEmpty()) throw RpcErrors.methodCallsRequired();

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Invocation invocation = Invocation.root();

		for (final MethodCall call : calls) {
			String name = call.getMethod();
			path.append(path.length() == 0 ? "" : ".").append(name);
			if (d == null) throw RpcErrors.methodNotFound(path);

			MethodDescriptor method = d.getMethods().get(name);
			if (method == null) throw RpcErrors.methodNotFound(path);

			try {
				invocation = method.parse(invocation, call.getArgs());
			} catch (Exception e) {
				throw RpcErrors.wrongMethodArgs(path);
			}

			if (!invocation.isRemote()) d = invocation.getNext();
		}

		if (!invocation.isRemote()) throw RpcErrors.notRemoteMethod(path);
		return invocation;
	}

	/** Serializes a remote proxy result. */
	@VisibleForTesting
	@SuppressWarnings("unchecked")
	static Response serializeResult(final Invocation remote, final Object result) {
		Descriptor resultDescriptor = remote.getResult();
		Object response = resultDescriptor.serialize(result);
		return Response.builder()
				.setResult(response)
				.setStatus(ResponseStatus.OK)
				.build();
	}

	/** Serializes a remote proxy exception or propagates the exception. */
	@VisibleForTesting
	@SuppressWarnings("unchecked")
	static Response exceptionResponseOrPropagate(final Invocation invocation, final Exception e) {
		Descriptor excDescriptor = invocation.getExc();
		if (excDescriptor == null || !excDescriptor.getJavaClass().isInstance(e)) {
			throw Throwables.propagate(e);
		}

		// It's an application exception.
		Object result = excDescriptor.serialize(e);
		return Response.builder()
				.setStatus(ResponseStatus.EXCEPTION)
				.setResult(result)
				.build();
	}

	/** Serializes an internal server error. */
	@VisibleForTesting
	static Response errorResponse(final Exception e) {
		RpcError error = RpcErrors.fromException(e);
		Object result = error.serialize();
		return Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(result)
				.build();
	}

	static class RequestFilter<T> extends AbstractFilter<Request, Response, Invocation, Object> {
		private final InterfaceDescriptor<T> descriptor;

		RequestFilter(final InterfaceDescriptor<T> descriptor) {
			this.descriptor = checkNotNull(descriptor);
		}

		@Override
		public Response apply(final Request input, final Function<Invocation, Object> next) {
			return handle(input, descriptor, next);
		}
	}
}
