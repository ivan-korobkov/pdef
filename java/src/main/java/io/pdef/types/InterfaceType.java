package io.pdef.types;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InterfaceType extends Type {
	private final Supplier<MessageType> exc;
	private final List<InterfaceMethod> declaredMethods;
	private final InterfaceMethod indexMethod;

	private InterfaceType(final Builder builder) {
		super(TypeEnum.INTERFACE, builder.javaClass);

		exc = builder.exc;
		declaredMethods = buildDeclaredMethods(builder, this);
		indexMethod = findIndexMethod(declaredMethods);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	@Override
	public TypeEnum getType() {
		return TypeEnum.INTERFACE;
	}

	@Nullable
	public MessageType getExc() {
		return exc == null ? null : exc.get();
	}

	public List<InterfaceMethod> getDeclaredMethods() {
		return declaredMethods;
	}

	public List<InterfaceMethod> getMethods() {
		return declaredMethods;
	}

	@Nullable
	public InterfaceMethod getIndexMethod() {
		return indexMethod;
	}

	@Nullable
	public InterfaceMethod findMethod(final String name) {
		for (InterfaceMethod method : getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	/** Returns an interface type or null. */
	@Nullable
	public static <T> InterfaceType findType(final Class<T> cls) {
		if (!cls.isInterface()) return null;

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
			return (InterfaceType) field.get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Builder {
		private Class<?> javaClass;
		private Supplier<MessageType> exc;
		private final List<InterfaceMethod.Builder> declaredMethods;

		private Builder() {
			declaredMethods = Lists.newArrayList();
		}

		public Builder setJavaClass(final Class<?> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder setExc(final Supplier<MessageType> exc) {
			this.exc = exc;
			return this;
		}

		public Builder addMethod(final InterfaceMethod.Builder method) {
			declaredMethods.add(method);
			return this;
		}

		public InterfaceType build() {
			return new InterfaceType(this);
		}
	}

	private static ImmutableList<InterfaceMethod> buildDeclaredMethods(final Builder builder,
			final InterfaceType iface) {
		ImmutableList.Builder<InterfaceMethod> temp = ImmutableList.builder();
		for (InterfaceMethod.Builder mb : builder.declaredMethods) {
			temp.add(mb.build(iface));
		}
		return temp.build();
	}

	private static InterfaceMethod findIndexMethod(final List<InterfaceMethod> methods) {
		for (InterfaceMethod method : methods) {
			if (method.isIndex()) {
				return method;
			}
		}
		return null;
	}
}
