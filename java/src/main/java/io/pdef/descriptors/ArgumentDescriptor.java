package io.pdef.descriptors;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ArgumentDescriptor provides a method argument name and type.
 * @param <V> Argument class.
 */
public class ArgumentDescriptor<V> {
	private final String name;
	private final DataDescriptor<V> type;

	public static <V> ArgumentDescriptor<V> of(final String name,
			final DataDescriptor<V> type) {
		return new ArgumentDescriptor<V>(name, type);
	}

	public ArgumentDescriptor(final String name, final DataDescriptor<V> type) {
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

	public DataDescriptor<V> getType() {
		return type;
	}
}
