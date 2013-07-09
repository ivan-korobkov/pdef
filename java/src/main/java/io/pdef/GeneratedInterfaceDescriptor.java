package io.pdef;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/** Abstract class for a generated interface descriptor. */
public class GeneratedInterfaceDescriptor<T> implements InterfaceDescriptor<T> {
	private final Class<T> javaClass;
	private final Constructor<T> constructor;

	@SuppressWarnings("unchecked")
	public GeneratedInterfaceDescriptor(final Class<T> javaClass) {
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
	public T proxy(final InvocationHandler handler) {
		try {
			return constructor.newInstance(handler);
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
}
