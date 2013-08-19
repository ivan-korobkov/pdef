package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.TypeEnum;

import javax.annotation.Nullable;
import java.util.List;

public class InterfaceDescriptor implements Descriptor {
	private final Class<?> cls;
	private final InterfaceDescriptor base;
	private final Supplier<Descriptor> exc;
	private final List<MethodDescriptor> declaredMethods;
	private List<MethodDescriptor> cachedMethods;

	private InterfaceDescriptor(final Builder builder) {
		cls = checkNotNull(builder.cls);
		base = builder.base;
		exc = builder.exc;

		ImmutableList.Builder<MethodDescriptor> temp = ImmutableList.builder();
		for (MethodDescriptor.Builder mb : builder.declaredMethods) {
			temp.add(mb.build(this));
		}
		declaredMethods = temp.build();
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
	public Descriptor getExc() {
		return exc == null ? null : exc.get();
	}

	public List<MethodDescriptor> getDeclaredMethods() {
		return declaredMethods;
	}

	public List<MethodDescriptor> getInheritedMethods() {
		return base != null ? base.getMethods() : ImmutableList.<MethodDescriptor>of();
	}

	public List<MethodDescriptor> getMethods() {
		if (cachedMethods == null) {
			cachedMethods = ImmutableList.<MethodDescriptor>builder()
					.addAll(getInheritedMethods())
					.addAll(getDeclaredMethods())
					.build();
		}
		return cachedMethods;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Class<?> cls;
		private InterfaceDescriptor base;
		private Supplier<Descriptor> exc;
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

		public Builder setExc(final Supplier<Descriptor> exc) {
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
