package io.pdef;

import static com.google.common.base.Preconditions.checkNotNull;

class ReflectionServer implements InvocationHandler {
	private final InterfaceDescriptor descriptor;
	private final Object delegate;

	ReflectionServer(final InterfaceDescriptor descriptor, final Object delegate) {
		this.descriptor = checkNotNull(descriptor);
		this.delegate = checkNotNull(delegate);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T apply(final Invocation invocation) {
		checkNotNull(invocation);
		String name = invocation.getMethod();
		MethodDescriptor method = descriptor.getMethods().get(name);
		return (T) method.invoke(delegate, invocation);
	}
}
