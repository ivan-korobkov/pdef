package io.pdef.descriptors;

/**
 * ArgumentDescriptor provides a method argument name and type.
 * @param <V> Argument class.
 */
public class ImmutableArgumentDescriptor<V> implements ArgumentDescriptor<V> {
	private final String name;
	private final ValueDescriptor<V> type;

	public static <V> ArgumentDescriptor<V> of(final String name,
			final ValueDescriptor<V> type) {
		return new ImmutableArgumentDescriptor<V>(name, type);
	}

	public ImmutableArgumentDescriptor(final String name, final ValueDescriptor<V> type) {
		if (name == null) throw new NullPointerException("name");
		if (type == null) throw new NullPointerException("type");

		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return "ArgumentDescriptor{'" + name + '\'' + ", " + type + '}';
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ValueDescriptor<V> getType() {
		return type;
	}

	@Override
	public boolean isPost() {
		return false;
	}

	@Override
	public boolean isQuery() {
		return false;
	}
}
