package io.pdef.descriptors;

/**
 * ArgumentDescriptor provides a method argument name and type.
 *
 * @param <V> Argument class.
 */
public class ArgumentDescriptor<V> {
	private final String name;
	private final ValueDescriptor<V> type;
	private final boolean query;
	private final boolean post;

	public static <V> ArgumentDescriptor<V> of(final String name,
			final ValueDescriptor<V> type) {
		return new ArgumentDescriptor<V>(name, type, false, false);
	}

	public ArgumentDescriptor(final String name, final ValueDescriptor<V> type,
			final boolean query, final boolean post) {
		if (name == null) throw new NullPointerException("name");
		if (type == null) throw new NullPointerException("type");

		this.name = name;
		this.type = type;
		this.query = query;
		this.post = post;
	}

	@Override
	public String toString() {
		return "ArgumentDescriptor{'" + name + '\'' + ", " + type + '}';
	}

	/** Returns a method argument name. */
	public String getName() {
		return name;
	}

	/** Returns an argument type descriptor. */
	public ValueDescriptor<V> getType() {
		return type;
	}

	/** Returns whether this argument is an HTTP post argument. */
	public boolean isPost() {
		return post;
	}

	/** Returns whether this argument is an HTTP query argument. */
	public boolean isQuery() {
		return query;
	}
}
