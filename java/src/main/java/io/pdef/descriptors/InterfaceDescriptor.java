package io.pdef.descriptors;

import io.pdef.Message;
import io.pdef.TypeEnum;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceDescriptor<T> extends Descriptor<T> {
	private final List<MethodDescriptor<T, ?>> methods;
	private final Map<String, MethodDescriptor<T, ?>> methodMap;
	private final MessageDescriptor<? extends Message> exc;

	private InterfaceDescriptor(final Builder<T> builder) {
		super(TypeEnum.INTERFACE, builder.javaClass);

		exc = builder.exc;
		methods = ImmutableCollections.list(builder.methods);
		methodMap = ImmutableCollections.map(methodsToMap(methods));
	}

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	@Override
	public String toString() {
		return "InterfaceDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	/** Returns a list of method descriptors or an empty list. */
	public List<MethodDescriptor<T, ?>> getMethods() {
		return methods;
	}

	/** Returns an exception descriptor or {@literal null}. */
	@Nullable
	public MessageDescriptor<? extends Message> getExc() {
		return exc;
	}

	/** Returns a method descriptor by its name and returns it or {@literal null}. */
	@Nullable
	public MethodDescriptor<T, ?> getMethod(final String name) {
		return methodMap.get(name);
	}

	public static class Builder<T> {
		private Class<T> javaClass;
		private MessageDescriptor<? extends Message> exc;
		private List<MethodDescriptor<T, ?>> methods;

		public Builder() {
			methods = new ArrayList<MethodDescriptor<T, ?>>();
		}

		public Builder<T> setJavaClass(final Class<T> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<T> setExc(final MessageDescriptor<? extends Message> exc) {
			this.exc = exc;
			return this;
		}

		public Builder<T> addMethod(final MethodDescriptor<T, ?> method) {
			this.methods.add(method);
			return this;
		}

		public InterfaceDescriptor<T> build() {
			return new InterfaceDescriptor<T>(this);
		}
	}

	private static <T> Map<String, MethodDescriptor<T, ?>> methodsToMap(
			final List<MethodDescriptor<T, ?>> methods) {
		Map<String, MethodDescriptor<T, ?>> map = new HashMap<String,
				MethodDescriptor<T, ?>>();
		for (MethodDescriptor<T, ?> method : methods) {
			map.put(method.getName(), method);
		}
		return map;
	}
}
