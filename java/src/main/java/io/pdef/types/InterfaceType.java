package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InterfaceType<T> extends MetaType {
	private final Class<T> javaClass;
	private final List<InterfaceMethod> methods;
	private final MessageType<?> exc;
	private final InterfaceMethod indexMethod;

	private InterfaceType(final Builder<T> builder) {
		super(TypeEnum.INTERFACE);
		this.javaClass = checkNotNull(builder.javaClass);
		this.exc = builder.exc; // Must be set before building methods.
		this.methods = ImmutableList.copyOf(builder.methods);
		this.indexMethod = findIndexMethod(methods);
	}

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	private static InterfaceMethod findIndexMethod(final List<InterfaceMethod> methods) {
		for (InterfaceMethod method : methods) {
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
	public List<InterfaceMethod> getMethods() {
		return methods;
	}

	/** Return an exception or null. */
	@Nullable
	public MessageType<?> getExc() {
		return exc;
	}

	/** Return an index method or null. */
	@Nullable
	public InterfaceMethod getIndexMethod() {
		return indexMethod;
	}

	/** Find a method by name and return it or null. */
	@Nullable
	public InterfaceMethod findMethod(final String name) {
		for (InterfaceMethod method : getMethods()) {
			if (method.name().equals(name)) {
				return method;
			}
		}

		return null;
	}

	public static class Builder<T> {
		private Class<T> javaClass;
		private MessageType<?> exc;
		private List<InterfaceMethod> methods;

		public Builder() {
			methods = Lists.newArrayList();
		}

		public Builder<T> setJavaClass(final Class<T> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<T> setExc(final MessageType<?> exc) {
			this.exc = exc;
			return this;
		}

		public Builder<T> addMethod(final InterfaceMethod method) {
			this.methods.add(method);
			return this;
		}

		public InterfaceType<T> build() {
			return new InterfaceType<T>(this);
		}
	}

	/** Returns an interface metatype or null. */
	@Nullable
	public static <T> InterfaceType<T> findMetaType(final Class<T> cls) {
		if (!cls.isInterface()) {
			return null;
		}

		Field field;
		try {
			field = cls.getField("META_TYPE");
		} catch (NoSuchFieldException e) {
			return null;
		}

		if (!Modifier.isStatic(field.getModifiers())) {
			return null;
		}
		if (field.getType() != InterfaceType.class) {
			return null;
		}

		try {
			// Get the static TYPE field.
			@SuppressWarnings("unchecked")
			InterfaceType<T> type = (InterfaceType<T>) field.get(null);
			return type;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
