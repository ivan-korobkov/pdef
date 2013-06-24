package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class GeneratedInterfaceDescriptor<T> implements InterfaceDescriptor<T> {
	private final Class<T> javaClass;
	private final Class<T> proxyClass;

	@SuppressWarnings("unchecked")
	protected GeneratedInterfaceDescriptor(final Class<T> javaClass) {
		this.javaClass = checkNotNull(javaClass);
		proxyClass = (Class<T>) Proxy.getProxyClass(javaClass.getClassLoader(), javaClass);
	}

	@Override
	public Class<T> getJavaClass() {
		return javaClass;
	}

	@Override
	public T client(final InvocationHandler handler) {
		return client(handler, null);
	}

	/** Creates a reflection-based client. */
	@SuppressWarnings("unchecked")
	@Override
	public T client(final InvocationHandler handler, @Nullable final Invocation parent) {
		ClientProxy proxyHandler = new ClientProxy(this, handler, parent);
		Object proxy;
		try {
			proxy = proxyClass
					.getConstructor(new Class[]{java.lang.reflect.InvocationHandler.class})
					.newInstance(proxyHandler);
		} catch (InvocationTargetException e) {
			throw Throwables.propagate(e.getCause());
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		return getJavaClass().cast(proxy);
	}

	/** Creates a reflection-based server. */
	@Override
	public InvocationHandler server(final T delegate) {
		return new Server(this, delegate);
	}

	protected static GeneratedMethodDescriptor.Builder method(final String name) {
		return GeneratedMethodDescriptor.builder(name);
	}

	protected static ImmutableMap<String, MethodDescriptor> methods(MethodDescriptor... methods) {
		ImmutableMap.Builder<String, MethodDescriptor> builder = ImmutableMap.builder();
		for (MethodDescriptor method : methods) {
			builder.put(method.getName(), method);
		}
		return builder.build();
	}

	static class ClientProxy implements java.lang.reflect.InvocationHandler {
		private final InterfaceDescriptor<?> descriptor;
		private final InvocationHandler handler;
		@Nullable private final Invocation parent;

		ClientProxy(final InterfaceDescriptor descriptor, final InvocationHandler handler,
				@Nullable final Invocation parent) {
			this.descriptor = checkNotNull(descriptor);
			this.handler = checkNotNull(handler);
			this.parent = parent;
		}

		@Override
		public Object invoke(final Object o, final Method method, final Object[] objects)
				throws Throwable {
			String name = method.getName();
			MethodDescriptor m = descriptor.getMethods().get(name);
			if (m == null) return method.invoke(o, objects); // It must be the equals, etc. method.

			Invocation invocation = m.capture(parent, objects);
			if (m.isRemote()) return handler.apply(invocation);

			return new ClientProxy(m.getResultInterface(), handler, invocation);
		}
	}

	static class Server implements InvocationHandler {
		private final InterfaceDescriptor descriptor;
		private final Object delegate;

		Server(final InterfaceDescriptor descriptor, final Object delegate) {
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
}
