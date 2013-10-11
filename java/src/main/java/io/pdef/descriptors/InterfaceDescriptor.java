package io.pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InterfaceDescriptor<T> extends Descriptor {
	private final Class<T> javaClass;
	private final List<MethodDescriptor> methods;
	private final MessageDescriptor<?> exc;
	private final MethodDescriptor indexMethod;

	private InterfaceDescriptor(final Builder<T> builder) {
		super(TypeEnum.INTERFACE);
		this.javaClass = checkNotNull(builder.javaClass);
		this.exc = builder.exc; // Must be set before building methods.
		this.methods = ImmutableList.copyOf(builder.methods);
		this.indexMethod = findIndexMethod(methods);
	}

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	private static MethodDescriptor findIndexMethod(final List<MethodDescriptor> methods) {
		for (MethodDescriptor method : methods) {
			if (method.isIndex()) {
				return method;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	/** Return this interface Java class. */
	public Class<?> getJavaClass() {
		return javaClass;
	}

	/** Return a list of interface methods or an empty list. */
	public List<MethodDescriptor> getMethods() {
		return methods;
	}

	/** Return an exception or null. */
	@Nullable
	public MessageDescriptor<?> getExc() {
		return exc;
	}

	/** Return an index method or null. */
	@Nullable
	public MethodDescriptor getIndexMethod() {
		return indexMethod;
	}

	/** Find a method by name and return it or null. */
	@Nullable
	public MethodDescriptor findMethod(final String name) {
		for (MethodDescriptor method : getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	public static class Builder<T> {
		private Class<T> javaClass;
		private MessageDescriptor<?> exc;
		private List<MethodDescriptor> methods;

		public Builder() {
			methods = Lists.newArrayList();
		}

		public Builder<T> setJavaClass(final Class<T> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<T> setExc(final MessageDescriptor<?> exc) {
			this.exc = exc;
			return this;
		}

		public Builder<T> addMethod(final MethodDescriptor method) {
			this.methods.add(method);
			return this;
		}

		public InterfaceDescriptor<T> build() {
			return new InterfaceDescriptor<T>(this);
		}
	}

	/** Returns an interface descriptor or null. */
	@Nullable
	public static <T> InterfaceDescriptor<T> findDescriptor(final Class<T> cls) {
		if (!cls.isInterface()) {
			return null;
		}

		Field field;
		try {
			field = cls.getField("DESCRIPTOR");
		} catch (NoSuchFieldException e) {
			return null;
		}

		if (!Modifier.isStatic(field.getModifiers())) {
			return null;
		} else if (field.getType() != InterfaceDescriptor.class) {
			return null;
		}

		try {
			// Get the static TYPE field.
			@SuppressWarnings("unchecked")
			InterfaceDescriptor<T> descriptor = (InterfaceDescriptor<T>) field.get(null);
			return descriptor;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
