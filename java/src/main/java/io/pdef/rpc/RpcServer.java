package io.pdef.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import io.pdef.Invocation;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.func.FluentFilter;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RpcServer {
	private RpcServer() {}

	/** Handles an rpc request and returns an rpc response, converts (almost) all exceptions to
	 * error rpc responses.
	 * @throws NullPointerException if descriptor or invoker is null. */
	public static <T> RpcResponse apply(final RpcRequest request,
			final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> invoker) {
		checkNotNull(descriptor);
		checkNotNull(invoker);

		try {
			if (request == null) throw RpcErrors.badRequest();
			Invocation invocation = parse(descriptor, request);

			try {
				Object result = invoker.apply(invocation);
				return response(invocation, result);
			} catch (Exception e) {
				return exception(invocation, e);
			}
		} catch (Exception e) {
			return RpcResponses.error(e);
		}
	}

	/** Parses a request into an proxy chain. */
	@VisibleForTesting
	static Invocation parse(final InterfaceDescriptor<?> descriptor, final RpcRequest request) {
		checkNotNull(request);
		List<RpcCall> calls = request.getCalls();
		if (calls.isEmpty()) throw RpcErrors.methodCallsRequired();

		StringBuilder path = new StringBuilder();
		InterfaceDescriptor<?> d = descriptor;
		Invocation invocation = Invocation.root();

		for (final RpcCall call : calls) {
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
	static RpcResponse response(final Invocation invocation, final Object result) {
		Descriptor descriptor = invocation.getResult();

		@SuppressWarnings("unchecked")
		Object object = descriptor.serialize(result);
		return RpcResponses.ok(object);
	}

	/** Serializes a remote proxy exception or propagates the exception. */
	@VisibleForTesting
	static RpcResponse exception(final Invocation invocation, final Exception e) {
		Descriptor descriptor = invocation.getExc();
		if (descriptor == null || !descriptor.getJavaClass().isInstance(e)) {
			throw Throwables.propagate(e);
		}

		// It's an application exception.
		@SuppressWarnings("unchecked")
		Object result = descriptor.serialize(e);
		return RpcResponses.exception(result);
	}


	/** Creates an rpc function. */
	public static <T> Function<RpcRequest, RpcResponse> function(
			final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> invoker) {
		checkNotNull(descriptor);
		checkNotNull(invoker);

		return new Function<RpcRequest, RpcResponse>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcServer.class)
						.addValue(this)
						.toString();
			}

			@Override
			public RpcResponse apply(@Nullable final RpcRequest input) {
				return RpcServer.apply(input, descriptor, invoker);
			}
		};
	}

	/** Creates an rpc filter. */
	public static <T> FluentFilter<RpcRequest, RpcResponse, Invocation, Object> filter(
			final InterfaceDescriptor<T> descriptor) {
		checkNotNull(descriptor);

		return new FluentFilter<RpcRequest, RpcResponse, Invocation, Object>() {
			@Override
			public String toString() {
				return Objects.toStringHelper(RpcServer.class)
						.addValue(this)
						.toString();
			}

			@Override
			public RpcResponse apply(final RpcRequest input, final Function<Invocation, Object> next) {
				return RpcServer.apply(input, descriptor, next);
			}
		};
	}
}
