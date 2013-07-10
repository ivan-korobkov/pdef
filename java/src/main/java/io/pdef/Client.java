package io.pdef;

import com.google.common.base.Function;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;

/** Client protocols, filters and handlers. */
public class Client {
	private Client() {}

	/** Creates a new rpc invocation handler. */
	public static Function<Invocation, Object> rpcHandler(final Function<Request, Response> sender) {
		return new ClientRpcProtocol(sender);
	}

	/** Creates a new rpc invocation filter. */
	public static Filter<Invocation, Object, Request, Response> rpcFilter() {
		return new ClientRpcProtocol.Filter();
	}

	/** Creates a proxy client from a descriptor and an invocation handler. */
	public static <T> T create(final InterfaceDescriptor<T> descriptor,
			final Function<Invocation, Object> handler) {
		return descriptor.proxy(ClientProxyHandler.root(descriptor, handler));
	}
}
