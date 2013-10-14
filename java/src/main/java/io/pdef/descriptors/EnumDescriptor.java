package io.pdef.descriptors;

import java.util.*;

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

		this.values = Collections.unmodifiableList(Arrays.asList(javaClass.getEnumConstants()));

		Map<String, T> temp = new HashMap<String, T>();
		for (T value : values) {
			temp.put(value.name().toUpperCase(), value);
		}
		this.namesToValues = Collections.unmodifiableMap(temp);
	}

	@Override
	public String toString() {
		return "EnumDescriptor{" + getJavaClass().getSimpleName() + '}';
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
