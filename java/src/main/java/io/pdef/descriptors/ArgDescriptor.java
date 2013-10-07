package io.pdef.descriptors;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class ArgDescriptor {
	private final String name;
	private final Supplier<DataDescriptor> type;
	private final MethodDescriptor method;

	private ArgDescriptor(final Builder builder, final MethodDescriptor method) {
		this.method = method;
		name = checkNotNull(builder.name);
		type = checkNotNull(builder.type);
	}

	public String getName() {
		return name;
	}

	public DataDescriptor getType() {
		return type.get();
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Supplier<DataDescriptor> type;

		private Builder() {}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setType(final Supplier<DataDescriptor> type) {
			this.type = type;
			return this;
		}

		@VisibleForTesting
		public Builder setType(final DataDescriptor type) {
			return setType(Suppliers.ofInstance(checkNotNull(type)));
		}

		public ArgDescriptor build(final MethodDescriptor method) {
			return new ArgDescriptor(this, method);
		}

		@VisibleForTesting
		public ArgDescriptor build() {
			return new ArgDescriptor(this, null);
		}
	}
}
