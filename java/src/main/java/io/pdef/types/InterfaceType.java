package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public abstract class InterfaceType<T> extends Type<T> {
	protected InterfaceType() {
		super(TypeEnum.INTERFACE);
	}

	/** Return this interface Java class. */
	public abstract Class<?> getJavaClass();

	/** Return a list of interface methods or an empty list. */
	public abstract List<InterfaceMethod> getMethods();

	/** Return an exception or null. */
	@Nullable
	public abstract MessageType<?> getExc();

	/** Return an index method or null. */
	@Nullable
	public abstract InterfaceMethod getIndexMethod();

	/** Find a method by name and return it or null. */
	@Nullable
	public abstract InterfaceMethod findMethod(String name);

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	public static abstract class Forwarding<T> extends InterfaceType<T> {
		protected abstract InterfaceType<T> delegate();

		@Override
		public Class<?> getJavaClass() {
			return delegate().getJavaClass();
		}

		@Override
		public List<InterfaceMethod> getMethods() {
			return delegate().getMethods();
		}

		@Nullable
		@Override
		public MessageType<?> getExc() {
			return delegate().getExc();
		}

		@Nullable
		@Override
		public InterfaceMethod getIndexMethod() {
			return delegate().getIndexMethod();
		}

		@Nullable
		@Override
		public InterfaceMethod findMethod(final String name) {
			return delegate().findMethod(name);
		}
	}

	private static class Immutable<T> extends InterfaceType<T> {
		private final Class<T> javaClass;
		private final List<InterfaceMethod> methods;
		private final Supplier<MessageType<?>> exc;
		private final InterfaceMethod indexMethod;

		private Immutable(final Builder<T> builder) {
			this.javaClass = checkNotNull(builder.javaClass);
			this.exc = builder.exc; // Must be set before building methods.
			this.methods = buildMethods(this, builder.methods);
			this.indexMethod = findIndexMethod(methods);
		}

		private static List<InterfaceMethod> buildMethods(final Immutable<?> type,
				final List<InterfaceMethod.Builder> builders) {
			ImmutableList.Builder<InterfaceMethod> methods = ImmutableList.builder();

			for (InterfaceMethod.Builder builder : builders) {
				builder.setExc(type.exc);
				builder.reflectionInvoker(type.javaClass);
				methods.add(builder.build());
			}

			return methods.build();
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
		public Class<?> getJavaClass() {
			return javaClass;
		}

		@Override
		public List<InterfaceMethod> getMethods() {
			return methods;
		}

		@Nullable
		@Override
		public MessageType<?> getExc() {
			return exc.get();
		}

		@Nullable
		@Override
		public InterfaceMethod getIndexMethod() {
			return indexMethod;
		}

		@Nullable
		@Override
		public InterfaceMethod findMethod(final String name) {
			for (InterfaceMethod method : getMethods()) {
				if (method.name().equals(name)) {
					return method;
				}
			}

			return null;
		}
	}

	public static class Builder<T> {
		private Class<T> javaClass;
		private Supplier<MessageType<?>> exc;
		private List<InterfaceMethod.Builder> methods;

		public Builder() {
			methods = Lists.newArrayList();
		}

		public Builder<T> setJavaClass(final Class<T> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<T> setExc(final Supplier<MessageType<?>> exc) {
			this.exc = exc;
			return this;
		}

		public Builder<T> addMethod(final InterfaceMethod.Builder method) {
			this.methods.add(method);
			return this;
		}

		public InterfaceType<T> build() {
			return new Immutable<T>(this);
		}
	}

	/** Returns an interface type or null. */
	@Nullable
	public static <T> InterfaceType<T> findType(final Class<T> cls) {
		if (!cls.isInterface()) {
			return null;
		}

		Field field;
		try {
			field = cls.getField("TYPE");
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
