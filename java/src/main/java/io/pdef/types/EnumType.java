package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class EnumType<T extends Enum<?>> extends DataType<T> {
	private final Class<T> javaClass;
	private final List<T> values;
	private final BiMap<T, String> valuesToNames;

	public EnumType(final Class<T> javaClass) {
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
	protected T doParseNative(final Object object) throws Exception {
		if (object == null) {
			return null;
		}

		if (object instanceof String) {
			return doParseString((String) object);
		}

		return javaClass.cast(object);
	}

	@Override
	protected String doToNative(final T value) throws Exception {
		return value == null ? null : doToString(value);
	}

	// String format.

	@Override
	protected T doParseString(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		String upper = s.toLowerCase();
		return valuesToNames.inverse().get(upper);
	}

	@Override
	protected String doToString(final T value) throws Exception {
		if (value == null) {
			return null;
		}

		return valuesToNames.get(value).toLowerCase();
	}
}
