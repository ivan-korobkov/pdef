package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import io.pdef.rpc.*;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Server<T> implements Function<Request, Response> {
	private final InterfaceDescriptor<T> descriptor;
	private final Supplier<T> supplier;

	public Server(final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		this.descriptor = checkNotNull(descriptor);
		this.supplier = checkNotNull(supplier);
	}

	@Override
	public Response apply(final Request request) {
		try {
			if (request == null) throw RpcErrors.badRequest();

			Object result;
			Invocation invocation = parseInvocation(request);
			try {
				result = invoke(invocation);
			} catch (Exception e) {
				return serializeExceptionOrRethrow(invocation.getExc(), e);
			}

			return serializeResult(invocation.getResult(), result);
		} catch (Exception e) {
			return serializeError(e);
		}
	}

	public Invocation parseInvocation(final Request request) {
		checkNotNull(request);
		List<MethodCall> calls = request.getCalls();

		InterfaceDescriptor<?> d = descriptor;
		StringBuilder path = new StringBuilder();

		Invocation invocation = Invocation.root();
		for (MethodCall call : calls) {
			String name = call.getMethod();
			path.append(path.length() == 0 ? "" : ".").append(name);

			MethodDescriptor method = d.getMethods().get(name);
			if (method == null) throw RpcErrors.methodNotFound(name);

			invocation = method.parse(invocation, call.getArgs());
			// TODO: next interface descriptor.
		}

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

	public Response serializeResult(final Descriptor<Object> resultDescriptor, final Object result) {
		Object response = resultDescriptor.serialize(result);
		return Response.builder()
				.setResult(response)
				.setStatus(ResponseStatus.OK)
				.build();
	}

	public Response serializeExceptionOrRethrow(final Descriptor<Object> excDescriptor,
			final Exception e) {
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
