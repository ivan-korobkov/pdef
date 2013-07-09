package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Server {
	private Server() {}

	/** Creates a default server from a descriptor and a service instance. */
	public static <T> Function<Request, Response> create(final InterfaceDescriptor<T> descriptor,
			final T service) {
		checkNotNull(service);
		return create(descriptor, Suppliers.ofInstance(service));
	}

	/** Creates a default server from a descriptor and a service supplier. */
	public static <T> Function<Request, Response> create(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		checkNotNull(descriptor);
		checkNotNull(supplier);
		return new Function<Request, Response>() {
			@Override
			public Response apply(final Request request) {
				try {
					if (request == null) throw RpcErrors.badRequest();

					Object result;
					Invocation invocation = parseRequest(descriptor, request);
					try {
						result = invoke(supplier, invocation);
					} catch (Exception e) {
						return serializeExceptionOrRethrow(invocation, e);
					}

					return serializeResult(invocation, result);
				} catch (Exception e) {
					return serializeError(e);
				}
			}
		};
	}

	/** Parses a request into an invocation chain. */
	public static <T> Invocation parseRequest(final InterfaceDescriptor<T> descriptor,
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

	/** Invokes an invocation chain on a service. */
	public static <T> Object invoke(final Supplier<T> supplier, final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		Object object = supplier.get();

		List<Invocation> invocations = remote.toList();
		for (Invocation invocation : invocations) {
			object = invocation.invoke(object);
		}

		return object;
	}

	/** Serializes a remote invocation result. */
	@SuppressWarnings("unchecked")
	public static Response serializeResult(final Invocation remote, final Object result) {
		Descriptor resultDescriptor = remote.getResult();
		Object response = resultDescriptor.serialize(result);
		return Response.builder()
				.setResult(response)
				.setStatus(ResponseStatus.OK)
				.build();
	}

	/** Serializes a remote invocation exception or propagates the exception. */
	@SuppressWarnings("unchecked")
	public static Response serializeExceptionOrRethrow(final Invocation invocation,
			final Exception e) {
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
	public static Response serializeError(final Exception e) {
		RpcError error = e instanceof RpcError ? (RpcError) e : RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
		Object result = error.serialize();
		return Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(result)
				.build();
	}
}
