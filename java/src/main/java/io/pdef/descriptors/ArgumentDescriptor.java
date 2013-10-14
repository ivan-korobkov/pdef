package io.pdef.descriptors;

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
		this.name = name;
		this.type = type;

		if (name == null) throw new NullPointerException("name");
		if (type == null) throw new NullPointerException("type");
	}

	@Override
	public String toString() {
		return "ArgumentDescriptor{'" + name + '\'' + ", " + type + '}';
	}

	public String getName() {
		return name;
	}

	public DataDescriptor<V> getType() {
		return type;
	}
}
