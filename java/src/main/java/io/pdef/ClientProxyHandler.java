package io.pdef;

import com.google.common.base.Function;

import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

class ClientProxyHandler implements java.lang.reflect.InvocationHandler {
	private final Invocation parent;
	private final InterfaceDescriptor<?> descriptor;
	private final Function<Invocation, Object> handler;

	public static ClientProxyHandler root(final InterfaceDescriptor<?> descriptor,
			final Function<Invocation, Object> handler) {
		Invocation root = Invocation.root();
		return new ClientProxyHandler(root, descriptor, handler);
	}

	ClientProxyHandler(final Invocation parent, final InterfaceDescriptor<?> descriptor,
			final Function<Invocation, Object> handler) {
		this.parent = checkNotNull(parent);
		this.descriptor = checkNotNull(descriptor);
		this.handler = checkNotNull(handler);
	}

	@Override
	public Object invoke(final Object o, final Method method, final Object[] args)
			throws Throwable {
		Map<String, MethodDescriptor> methods = descriptor.getMethods();
		if (!methods.containsKey(method.getName())) return method.invoke(this, args);

		MethodDescriptor descriptor = methods.get(method.getName());
		Invocation invocation = descriptor.capture(parent, args != null ? args : new Object[0]);
		if (invocation.isRemote()) return handler.apply(invocation);

		return new ClientProxyHandler(parent, descriptor.getNext(), handler);
	}
}
