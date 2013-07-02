package io.pdef;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.Map;

public class GeneratedInterfaceDescriptor<T> implements InterfaceDescriptor<T> {
	private final Class<T> javaClass;

	public GeneratedInterfaceDescriptor(final Class<T> javaClass) {
		this.javaClass = checkNotNull(javaClass);
	}

	@Override
	public Class<T> getJavaClass() {
		return javaClass;
	}

	@Override
	public Map<String, MethodDescriptor> getMethods() {
		return ImmutableMap.of();
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
