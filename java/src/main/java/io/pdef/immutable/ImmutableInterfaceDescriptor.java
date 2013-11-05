package io.pdef.immutable;

import io.pdef.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ImmutableInterfaceDescriptor<T> extends AbstractDescriptor<T>
		implements InterfaceDescriptor<T> {
	private final List<MethodDescriptor<T, ?>> methods;
	private final MessageDescriptor<?> exc;
	private final MethodDescriptor<T, ?> indexMethod;

	private ImmutableInterfaceDescriptor(final Builder<T> builder) {
		super(TypeEnum.INTERFACE, builder.javaClass);
		this.exc = builder.exc;
		this.methods = ImmutableCollections.list(builder.methods);
		this.indexMethod = findIndexMethod(methods);
	}

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	@Override
	public String toString() {
		return "InterfaceDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	@Override
	public List<MethodDescriptor<T, ?>> getMethods() {
		return methods;
	}

	@Override
	@Nullable
	public MessageDescriptor<?> getExc() {
		return exc;
	}

	@Override
	@Nullable
	public MethodDescriptor<T, ?> getIndexMethod() {
		return indexMethod;
	}

	@Override
	@Nullable
	public MethodDescriptor<T, ?> findMethod(final String name) {
		for (MethodDescriptor<T, ?> method : getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	public static class Builder<T> {
		private Class<T> javaClass;
		private MessageDescriptor<?> exc;
		private List<MethodDescriptor<T, ?>> methods;

		public Builder() {
			methods = new ArrayList<MethodDescriptor<T, ?>>();
		}

		public Builder<T> setJavaClass(final Class<T> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<T> setExc(final MessageDescriptor<?> exc) {
			this.exc = exc;
			return this;
		}

		public Builder<T> addMethod(final MethodDescriptor<T, ?> method) {
			this.methods.add(method);
			return this;
		}

		public InterfaceDescriptor<T> build() {
			return new ImmutableInterfaceDescriptor<T>(this);
		}
	}

	private static <T> MethodDescriptor<T, ?> findIndexMethod(
			final List<MethodDescriptor<T, ?>> methods) {
		for (MethodDescriptor<T, ?> method : methods) {
			if (method.isIndex()) {
				return method;
			}
		}

		return null;
	}
}
