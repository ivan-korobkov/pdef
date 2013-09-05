package pdef.descriptors;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.TypeEnum;

import java.util.List;

public class EnumDescriptor extends PrimitiveDescriptor {
	private final List<Enum<?>> values;
	private final BiMap<Enum<?>, String> valuesToNames;

	private EnumDescriptor(final Builder builder) {
		super(TypeEnum.ENUM);

		values = ImmutableList.copyOf(builder.values);
		valuesToNames = buildValuesToNames(values);
	}

	public List<Enum<?>> getValues() {
		return values;
	}

	@Override
	public Enum<?> parseObject(final Object o) {
		if (o == null) {
			return null;
		}

		if (o instanceof String) {
			return parseString((String) o);
		}
		return ((Enum<?>) o);
	}

	@Override
	public Object toObject(final Object o) {
		return o == null ? null : toString(o);
	}

	@Override
	public Enum<?> parseString(final String s) {
		return s == null ? null : valuesToNames.inverse().get(s.toUpperCase());
	}

	@Override
	public String toString(final Object o) {
		return o == null ? null : ((Enum<?>) o).name().toLowerCase();
	}

	/** Creates an enum descriptor builder. */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Enum<?>> values;

		private Builder() {
			this.values = Lists.newArrayList();
		}

		public Builder addValue(final Enum<?> value) {
			values.add(value);
			return this;
		}

		public EnumDescriptor build() {
			return new EnumDescriptor(this);
		}
	}

	private static ImmutableBiMap<Enum<?>, String> buildValuesToNames(final List<Enum<?>> values) {
		ImmutableBiMap.Builder<Enum<?>, String> builder = ImmutableBiMap.builder();
		for (Enum<?> value : values) {
			builder.put(value, value.name().toUpperCase());
		}
		return builder.build();
	}
}
