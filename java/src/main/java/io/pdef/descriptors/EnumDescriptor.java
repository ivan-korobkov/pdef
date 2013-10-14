package io.pdef.descriptors;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * EnumDescriptor holds enum values and parsing/serialization methods.
 * */
public class EnumDescriptor<T extends Enum<T>> extends DataDescriptor<T> {
	private final List<T> values;
	private final Map<String, T> namesToValues;

	public static <T extends Enum<T>> EnumDescriptor<T> of(final Class<T> javaClass) {
		return new EnumDescriptor<T>(javaClass);
	}

	private EnumDescriptor(final Class<T> javaClass) {
		super(TypeEnum.ENUM, javaClass);

		this.values = ImmutableList.copyOf(javaClass.getEnumConstants());

		ImmutableMap.Builder<String, T> temp = ImmutableMap.builder();
		for (T value : values) {
			temp.put(value.name().toUpperCase(), value);
		}
		this.namesToValues = temp.build();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass().getSimpleName())
				.toString();
	}

	/** Returns an immutable list of values. */
	public List<T> getValues() {
		return values;
	}

	/** Returns an immutable list of uppercase names to values. */
	public Map<String, T> getNamesToValues() {
		return namesToValues;
	}

	@Override
	public T copy(final T object) {
		return object;
	}
}
