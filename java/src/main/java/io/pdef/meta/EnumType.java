package io.pdef.meta;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * EnumType holds enum values and parsing/serialization methods.
 * */
public class EnumType<T extends Enum<?>> extends DataType<T> {
	private final Class<T> javaClass;
	private final List<T> values;
	private final BiMap<T, String> valuesToNames;

	public static <T extends Enum<?>> EnumType<T> of(final Class<T> javaClass) {
		return new EnumType<T>(javaClass);
	}

	private EnumType(final Class<T> javaClass) {
		super(TypeEnum.ENUM);

		this.javaClass = checkNotNull(javaClass);
		this.values = ImmutableList.copyOf(javaClass.getEnumConstants());
		this.valuesToNames = buildValuesToNames(values);
	}

	private static <T extends Enum<?>> ImmutableBiMap<T, String> buildValuesToNames(
			final Iterable<T> values) {
		ImmutableBiMap.Builder<T, String> builder = ImmutableBiMap.builder();
		for (T value : values) {
			builder.put(value, value.name().toLowerCase());
		}
		return builder.build();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(javaClass.getSimpleName())
				.toString();
	}

	public Class<T> getJavaClass() {
		return javaClass;
	}

	public List<T> getValues() {
		return values;
	}

	// Native format.

	@Override
	public T copy(final T object) {
		return object;
	}

	@Override
	protected T fromNative(final Object object) throws Exception {
		if (object == null) {
			return null;
		}

		if (object instanceof String) {
			return fromString((String) object);
		}

		return javaClass.cast(object);
	}

	@Override
	protected String toNative(final T value) throws Exception {
		return value == null ? null : toString(value);
	}

	// String format.

	@Override
	protected T fromString(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		String upper = s.toLowerCase();
		return valuesToNames.inverse().get(upper);
	}

	@Override
	protected String toString(final T value) throws Exception {
		if (value == null) {
			return null;
		}

		return valuesToNames.get(value).toLowerCase();
	}
}
