package io.pdef.descriptors;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.pdef.Invocation;
import io.pdef.InvocationResult;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Map;

/** Abstract class for a generated interface descriptor. */
public class GeneratedInterfaceDescriptor<T> implements InterfaceDescriptor<T> {
	private final Class<T> javaClass;
	private final Constructor<T> constructor;

	@SuppressWarnings("unchecked")
	protected GeneratedInterfaceDescriptor(final Class<T> javaClass) {
		this.javaClass = checkNotNull(javaClass);
		final Class<T> proxyClass = (Class<T>) Proxy
				.getProxyClass(javaClass.getClassLoader(), javaClass);
		try {
			constructor = proxyClass.getConstructor(InvocationHandler.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<T> getJavaClass() {
		return javaClass;
	}

	@Override
	public Map<String, MethodDescriptor> getMethods() {
		return ImmutableMap.of();
	}

	@Nullable
	@Override
	public MethodDescriptor getMethod(final String method) {
		return getMethods().get(method);
	}

	@Override
	public boolean hasMethod(final String name) {
		return getMethods().containsKey(name);
	}

	@Override
	public T client(final Function<Invocation, InvocationResult> handler) {
		checkNotNull(handler);
		return proxy(Invocation.root(), handler);
	}

	@Override
	public T client(final Invocation parent, final Function<Invocation, InvocationResult> handler) {
		checkNotNull(parent);
		checkNotNull(handler);
		return proxy(parent, handler);
	}

	private T proxy(final Invocation parent, final Function<Invocation, InvocationResult> handler) {
		InvocationHandler invocationHandler = new ProxyInvocationHandler(parent, handler, this);
		try {
			return constructor.newInstance(invocationHandler);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw Throwables.propagate(e.getCause());
		}
	}

	public static GeneratedMethodDescriptor.Builder method(
			final InterfaceDescriptor<?> iface, final String name) {
		return GeneratedMethodDescriptor.builder(iface, name);
	}

	public static Map<String, MethodDescriptor> methods(final MethodDescriptor... methods) {
		ImmutableMap.Builder<String, MethodDescriptor> builder = ImmutableMap.builder();
		for (MethodDescriptor method : methods) builder.put(method.getName(), method);
		return builder.build();
	}

	static Method getMethodByName(final Class<?> cls, final String name) {
		for (Method method : cls.getMethods()) if (method.getName().equals(name)) return method;
		throw new IllegalArgumentException("Method not found \"" + name + "\"");
	}

	static class ProxyInvocationHandler implements InvocationHandler {
		private final Invocation parent;
		private final Function<Invocation, InvocationResult> handler;
		private final InterfaceDescriptor<?> descriptor;

		ProxyInvocationHandler(final Invocation parent,
				final Function<Invocation, InvocationResult> handler,
				final InterfaceDescriptor<?> descriptor) {
			this.parent = checkNotNull(parent);
			this.handler = checkNotNull(handler);
			this.descriptor = checkNotNull(descriptor);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object invoke(final Object o, final Method method, final Object[] args)
				throws Throwable {
			String name = method.getName();
			if (!descriptor.hasMethod(name)) return method.invoke(this, args);

			Object[] argArray = args != null ? args : new Object[0];
			MethodDescriptor m = descriptor.getMethod(name);
			Invocation invocation = m.capture(parent, argArray);
			if (invocation.isRemote()) {
				InvocationResult result = handler.apply(invocation);
				assert result != null;
				if (result.isSuccess()) return result.getResult();
				throw (RuntimeException) result.getResult();
			}

			// It is not a remote invocation so it must have a next interface to call.
			InterfaceDescriptor<?> next = m.getNext();
			assert next != null;
			return next.client(invocation, handler);
		}
	}
}
