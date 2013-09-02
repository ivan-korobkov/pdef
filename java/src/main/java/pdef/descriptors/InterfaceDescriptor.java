package pdef.descriptors;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.TypeEnum;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class InterfaceDescriptor implements Descriptor {
	private final Class<?> cls;
	private final InterfaceDescriptor base;
	private final Supplier<MessageDescriptor> exc;
	private final List<MethodDescriptor> declaredMethods;
	private final List<MethodDescriptor> methods;
	private final MethodDescriptor indexMethod;

	private InterfaceDescriptor(final Builder builder) {
		cls = checkNotNull(builder.cls);
		base = builder.base;
		exc = builder.exc;

		declaredMethods = buildDeclaredMethods(builder, this);
		methods = buildMethods(declaredMethods, base);
		indexMethod = buildIndexMethod(methods);
	}

	private static ImmutableList<MethodDescriptor> buildDeclaredMethods(final Builder builder,
			final InterfaceDescriptor iface) {
		ImmutableList.Builder<MethodDescriptor> temp = ImmutableList.builder();
		for (MethodDescriptor.Builder mb : builder.declaredMethods) {
			temp.add(mb.build(iface));
		}
		return temp.build();
	}

	private static List<MethodDescriptor> buildMethods(final List<MethodDescriptor> declaredMethods,
			final InterfaceDescriptor base) {
		ImmutableList.Builder<MethodDescriptor> temp = ImmutableList.builder();
		if (base != null) {
			temp.addAll(base.getMethods());
		}

		temp.addAll(declaredMethods);
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
	public InterfaceDescriptor getBase() {
		return base;
	}

	@Nullable
	public MessageDescriptor getExc() {
		return exc == null ? null : exc.get();
	}

	public List<MethodDescriptor> getDeclaredMethods() {
		return declaredMethods;
	}

	public List<MethodDescriptor> getInheritedMethods() {
		return base != null ? base.getMethods() : ImmutableList.<MethodDescriptor>of();
	}

	public List<MethodDescriptor> getMethods() {
		return methods;
	}

	@Nullable
	public MethodDescriptor findMethod(final String name) {
		for (MethodDescriptor method : methods) {
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

	public static class Builder {
		private Class<?> cls;
		private InterfaceDescriptor base;
		private Supplier<MessageDescriptor> exc;
		private final List<MethodDescriptor.Builder> declaredMethods;

		private Builder() {
			declaredMethods = Lists.newArrayList();
		}

		public Builder setCls(final Class<?> cls) {
			this.cls = cls;
			return this;
		}

		public Builder setBase(final InterfaceDescriptor base) {
			this.base = base;
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
