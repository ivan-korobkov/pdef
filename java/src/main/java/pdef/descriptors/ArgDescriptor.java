package pdef.descriptors;

import static com.google.common.base.Preconditions.*;

public class ArgDescriptor {
	private final String name;
	private final boolean query;
	private final DescriptorSupplier type;
	private final MethodDescriptor method;

	private ArgDescriptor(final Builder builder, final MethodDescriptor method) {
		this.method = checkNotNull(method);
		name = checkNotNull(builder.name);
		query = builder.query;
		type = checkNotNull(builder.type);
	}

	public String getName() {
		return name;
	}

	public boolean isQuery() {
		return query;
	}

	public Descriptor getType() {
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
		private boolean query;
		private DescriptorSupplier type;

		private Builder() {}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setQuery(final boolean query) {
			this.query = query;
			return this;
		}

		public Builder setType(final DescriptorSupplier type) {
			this.type = type;
			return this;
		}

		public ArgDescriptor build(final MethodDescriptor method) {
			return new ArgDescriptor(this, method);
		}
	}
}
