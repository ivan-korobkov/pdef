package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;

public class InterfaceMethodArg<V> {
	private final String name;
	private final DataType<V> type;

	public static <V> InterfaceMethodArg<V> of(final String name, final DataType<V> type) {
		return new InterfaceMethodArg<V>(name, type);
	}

	public InterfaceMethodArg(final String name, final DataType<V> type) {
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

	public DataType<V> getType() {
		return type;
	}
}
