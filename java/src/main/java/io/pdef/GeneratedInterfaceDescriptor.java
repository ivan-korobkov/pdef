package io.pdef;

public abstract class GeneratedInterfaceDescriptor implements InterfaceDescriptor {

	@Override
	public Object client(final Invocation parent, final InvocationHandler handler) {
		return new ReflectionClient(this, parent, handler);
	}

	@Override
	public InvocationHandler server(final Object delegate) {
		return new ReflectionServer(this, delegate);
	}
}
