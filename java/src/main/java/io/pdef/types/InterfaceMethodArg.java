package io.pdef.types;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class InterfaceMethodArg {
	private final String name;
	private final Supplier<DataType> type;
	private final InterfaceMethod method;

	private InterfaceMethodArg(final Builder builder, final InterfaceMethod method) {
		this.method = method;
		name = checkNotNull(builder.name);
		type = checkNotNull(builder.type);
	}

	public String getName() {
		return name;
	}

	public DataType getType() {
		return type.get();
	}

	public InterfaceMethod getMethod() {
		return method;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Supplier<DataType> type;

		private Builder() {}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setType(final Supplier<DataType> type) {
			this.type = type;
			return this;
		}

		@VisibleForTesting
		public Builder setType(final DataType type) {
			return setType(Suppliers.ofInstance(checkNotNull(type)));
		}

		public InterfaceMethodArg build(final InterfaceMethod method) {
			return new InterfaceMethodArg(this, method);
		}

		@VisibleForTesting
		public InterfaceMethodArg build() {
			return new InterfaceMethodArg(this, null);
		}
	}
}
