package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class GeneratedInterfaceType<T> implements InterfaceType<T> {
	private final Class<T> javaClass;
	private final Class<T> proxyClass;

	@SuppressWarnings("unchecked")
	protected GeneratedInterfaceType(final Class<T> javaClass) {
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

	protected static GeneratedMethod.Builder method(final String name) {
		return GeneratedMethod.builder(name);
	}

	protected static ImmutableMap<String, Method> methods(Method... methods) {
		ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
		for (Method method : methods) {
			builder.put(method.getName(), method);
		}
		return builder.build();
	}

	static class ClientProxy implements java.lang.reflect.InvocationHandler {
		private final InterfaceType<?> descriptor;
		private final InvocationHandler handler;
		@Nullable private final Invocation parent;

		ClientProxy(final InterfaceType descriptor, final InvocationHandler handler,
				@Nullable final Invocation parent) {
			this.descriptor = checkNotNull(descriptor);
			this.handler = checkNotNull(handler);
			this.parent = parent;
		}

		@Override
		public Object invoke(final Object o, final java.lang.reflect.Method method, final Object[] objects)
				throws Throwable {
			String name = method.getName();
			Method m = descriptor.getMethods().get(name);
			if (m == null) return method.invoke(o, objects); // It must be the equals, etc. method.

			Invocation invocation = m.capture(parent, objects);
			if (m.isRemote()) return handler.apply(invocation);

			return new ClientProxy(m.getResultInterface(), handler, invocation);
		}
	}

	static class Server implements InvocationHandler {
		private final InterfaceType descriptor;
		private final Object delegate;

		Server(final InterfaceType descriptor, final Object delegate) {
			this.descriptor = checkNotNull(descriptor);
			this.delegate = checkNotNull(delegate);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T apply(final Invocation invocation) {
			checkNotNull(invocation);
			String name = invocation.getMethod();
			Map<String, Method> methods = descriptor.getMethods();
			Method method = methods.get(name);
			return (T) method.invoke(delegate, invocation);
		}
	}
}
