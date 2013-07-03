package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.pdef.rpc.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Client<T> implements Function<Invocation, Object> {
	private final InterfaceDescriptor<T> descriptor;
	private final Function<Request, Response> handler;

	protected Client(final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> handler) {
		this.descriptor = checkNotNull(descriptor);
		this.handler = checkNotNull(handler);
	}

	public static <T> Client<T> create(final InterfaceDescriptor<T> descriptor,
			final Function<Request, Response> handler) {
		return new Client<T>(descriptor, handler);
	}

	public T proxy() {
		Invocation root = Invocation.root();
		ProxyHandler invocationHandler = new ProxyHandler(root, descriptor, this);
		return descriptor.proxy(invocationHandler);
	}

	@Override
	public Object apply(final Invocation invocation) {
		checkNotNull(invocation);
		Request request = serializeInvocation(invocation);
		Response response = handler.apply(request);
		checkNotNull(response);

		if (response != null) {
			Object result = response.getResult();
			switch (response.getStatus()) {
				case OK:
					return invocation.getResult().parse(result);
				case EXCEPTION:
					throw (RuntimeException) invocation.getExc().parse(result);
				case ERROR:
					throw RpcError.parse(result);
			}
		}

		throw RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("No response status")
				.build();
	}

	public Request serializeInvocation(final Invocation remote) {
		checkArgument(remote.isRemote(), "must be a remote invocation, got %s", remote);
		List<Invocation> invocations = remote.toList();

		List<MethodCall> calls = Lists.newArrayList();
		for (Invocation invocation : invocations) calls.add(invocation.serialize());

		return Request.builder()
				.setCalls(calls)
				.build();
	}

	static class ProxyHandler implements java.lang.reflect.InvocationHandler {
		private final Invocation parent;
		private final InterfaceDescriptor<?> iface;
		private final Function<Invocation, Object> handler;

		ProxyHandler(final Invocation parent, final InterfaceDescriptor<?> iface,
				final Function<Invocation, Object> handler) {
			this.parent = checkNotNull(parent);
			this.iface = checkNotNull(iface);
			this.handler = checkNotNull(handler);
		}

		@Override
		public Object invoke(final Object o, final Method method, final Object[] args)
				throws Throwable {
			Map<String, MethodDescriptor> methods = iface.getMethods();
			if (!methods.containsKey(method.getName())) return method.invoke(this, args);

			MethodDescriptor descriptor = methods.get(method.getName());
			Invocation invocation = descriptor.capture(parent, args != null ? args : new Object[0]);
			if (invocation.isRemote()) return handler.apply(invocation);

			return new ProxyHandler(parent, descriptor.getNext(), handler);
		}
	}
}
