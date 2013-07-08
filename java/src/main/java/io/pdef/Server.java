package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Server<T> implements Function<Request, Response> {
	private final InterfaceDescriptor<T> descriptor;
	private final Supplier<T> supplier;

	protected Server(final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		this.descriptor = checkNotNull(descriptor);
		this.supplier = checkNotNull(supplier);
	}

	public static <T> Server<T> create(final InterfaceDescriptor<T> descriptor, final T instance) {
		return create(descriptor, Suppliers.ofInstance(instance));
	}

	public static <T> Server<T> create(final InterfaceDescriptor<T> descriptor,
			final Supplier<T> supplier) {
		return new Server<T>(descriptor, supplier);
	}

	public InterfaceDescriptor<T> getDescriptor() {
		return descriptor;
	}

	public Supplier<T> getSupplier() {
		return supplier;
	}

	@Override
	public Response apply(final Request request) {
		try {
			if (request == null) throw RpcErrors.badRequest();

			Object result;
			Invocation invocation = parseRequest(request);
			try {
				result = invoke(invocation);
			} catch (Exception e) {
				return serializeExceptionOrRethrow(invocation, e);
			}

			return serializeResult(invocation, result);
		} catch (Exception e) {
			return serializeError(e);
		}
	}

	public Invocation parseRequest(final Request request) {
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

	public Object invoke(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		Object object = supplier.get();

		List<Invocation> invocations = remote.toList();
		for (Invocation invocation : invocations) {
			object = invocation.invoke(object);
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	public Response serializeResult(final Invocation invocation, final Object result) {
		Descriptor resultDescriptor = invocation.getResult();
		Object response = resultDescriptor.serialize(result);
		return Response.builder()
				.setResult(response)
				.setStatus(ResponseStatus.OK)
				.build();
	}

	@SuppressWarnings("unchecked")
	public Response serializeExceptionOrRethrow(final Invocation invocation, final Exception e) {
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

	public Response serializeError(final Exception e) {
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
