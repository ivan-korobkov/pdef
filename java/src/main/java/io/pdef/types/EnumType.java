package io.pdef.types;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class EnumType extends DataType {
	private final List<Enum<?>> values;
	private final BiMap<Enum<?>, String> valuesToNames;

	public EnumType(final Class<? extends Enum<?>> javaClass) {
		super(TypeEnum.ENUM, javaClass);
		Enum<?>[] vv =javaClass.getEnumConstants();
		this.values = ImmutableList.copyOf(vv);
		this.valuesToNames = buildValuesToNames(values);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	public List<Enum<?>> getValues() {
		return values;
	}

	// Native format.

	@Override
	public Object copy(final Object object) {
		return object;
	}

	@Override
	protected Enum<?> doParseNative(final Object o) throws Exception {
		if (o == null) {
			return null;
		}

		if (o instanceof String) {
			return doParseString((String) o);
		}

		return ((Enum<?>) o);
	}

	@Override
	protected String doToNative(final Object o) throws Exception {
		return o == null ? null : doToString(o);
	}

	// String format.

	@Override
	protected Enum<?> doParseString(final String s) throws Exception {
		if (s == null) {
			return null;
		}

		String upper = s.toLowerCase();
		return valuesToNames.inverse().get(upper);
	}

	@Override
	protected String doToString(final Object o) throws Exception {
		if (o == null) {
			return null;
		}

		Enum<?> e = (Enum<?>) o;
		return valuesToNames.get(e).toLowerCase();
	}

	// Static utility methods.

	private static ImmutableBiMap<Enum<?>, String> buildValuesToNames(
			final Iterable<Enum<?>> values) {
		ImmutableBiMap.Builder<Enum<?>, String> builder = ImmutableBiMap.builder();
		for (Enum<?> value : values) {
			builder.put(value, value.name().toLowerCase());
		}
		return builder.build();
	}
}
