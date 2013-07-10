package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/** Server protocols, filters and handlers. */
public class Server {
	private Server() {}

	// === Http ===

	/** Creates a simple http rpc server as httpFilter.then(rpcFilter).then(create). */
	public static <T> Function<ServerHttpProtocol.RequestResponse, Void> httpServer(
			final InterfaceDescriptor<T> descriptor, final Supplier<T> supplier) {
		return httpFilter(descriptor)
				.then(rpcFilter(descriptor))
				.then(invocationHandler(supplier));
	}

	/** Creates an http handler. */
	public static <T> Function<ServerHttpProtocol.RequestResponse, Void> httpHandler(
			final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> requestHandler) {
		return new ServerHttpProtocol<T>(descriptor, requestHandler);
	}

	/** Creates an http filter. */
	public static <T> Filter<ServerHttpProtocol.RequestResponse, Void, Request, Response> httpFilter(
			final InterfaceDescriptor<T> descriptor) {
		return new ServerHttpProtocol.HttpFilter<T>(descriptor);
	}


	// === Rpc ===

	/** Creates a request handler. */
	public static <T> Function<Request, Response> rpcHandler(
			final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> invocationHandler) {
		return new ServerRpcProtocol<T>(descriptor, invocationHandler);
	}

	/** Creates a request filter. */
	public static <T> Filter<Request, Response, Invocation, Object> rpcFilter(
			final InterfaceDescriptor<T> descriptor) {
		return new ServerRpcProtocol.RequestFilter<T>(descriptor);
	}


	// === Invocation ===

	/** Creates an invocation handler from a service instance. */
	public static <T> Function<Invocation, Object> invocationHandler(final T service) {
		checkNotNull(service);
		return new ServerInvocationHandler<T>(Suppliers.ofInstance(service));
	}

	/** Creates an invocation handler from a service supplier. */
	public static <T> Function<Invocation, Object> invocationHandler(final Supplier<T> supplier) {
		return new ServerInvocationHandler<T>(supplier);
	}
}
