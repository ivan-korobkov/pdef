package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;

public class InterfaceMethodArg {
	private final String name;
	private final Supplier<DataType<?>> type;

	public InterfaceMethodArg(final String name, final Supplier<DataType<?>> type) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(getType())
				.toString();
	}

	public String getName() {
		return name;
	}

	public DataType<?> getType() {
		return type.get();
	}
}
