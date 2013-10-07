package io.pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.pdef.TypeEnum;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InterfaceDescriptor implements Descriptor {
	private final Class<?> cls;
	private final Supplier<MessageDescriptor> exc;
	private final List<MethodDescriptor> declaredMethods;
	private final MethodDescriptor indexMethod;

	private InterfaceDescriptor(final Builder builder) {
		cls = checkNotNull(builder.cls);
		exc = builder.exc;

		declaredMethods = buildDeclaredMethods(builder, this);
		indexMethod = buildIndexMethod(declaredMethods);
	}

	private static ImmutableList<MethodDescriptor> buildDeclaredMethods(final Builder builder,
			final InterfaceDescriptor iface) {
		ImmutableList.Builder<MethodDescriptor> temp = ImmutableList.builder();
		for (MethodDescriptor.Builder mb : builder.declaredMethods) {
			temp.add(mb.build(iface));
		}
		return temp.build();
	}

	private static MethodDescriptor buildIndexMethod(final List<MethodDescriptor> methods) {
		for (MethodDescriptor method : methods) {
			if (method.isIndex()) {
				return method;
			}
		}

		return null;
	}

	@Override
	public TypeEnum getType() {
		return TypeEnum.INTERFACE;
	}

	public Class<?> getCls() {
		return cls;
	}

	@Nullable
	public MessageDescriptor getExc() {
		return exc == null ? null : exc.get();
	}

	public List<MethodDescriptor> getDeclaredMethods() {
		return declaredMethods;
	}

	public List<MethodDescriptor> getMethods() {
		return declaredMethods;
	}

	@Nullable
	public MethodDescriptor findMethod(final String name) {
		for (MethodDescriptor method : getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	@Nullable
	public MethodDescriptor getIndexMethod() {
		return indexMethod;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Returns an interface class descriptor if present. */
	public static <T> InterfaceDescriptor findDescriptor(final Class<T> cls) {
		if (!cls.isInterface()) return null;

		Field field;
		try {
			field = cls.getField("DESCRIPTOR");
		} catch (NoSuchFieldException e) {
			return null;
		}

		if (!Modifier.isStatic(field.getModifiers())) return null;
		if (field.getType() != InterfaceDescriptor.class) return null;

		try {
			return (InterfaceDescriptor) field.get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Builder {
		private Class<?> cls;
		private Supplier<MessageDescriptor> exc;
		private final List<MethodDescriptor.Builder> declaredMethods;

		private Builder() {
			declaredMethods = Lists.newArrayList();
		}

		public Builder setCls(final Class<?> cls) {
			this.cls = cls;
			return this;
		}

		public Builder setExc(final Supplier<MessageDescriptor> exc) {
			this.exc = exc;
			return this;
		}

		public Builder addMethod(final MethodDescriptor.Builder method) {
			declaredMethods.add(method);
			return this;
		}

		public InterfaceDescriptor build() {
			return new InterfaceDescriptor(this);
		}
	}
}
