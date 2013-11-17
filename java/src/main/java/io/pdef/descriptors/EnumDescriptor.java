package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** EnumDescriptor holds enum values and parsing/serialization methods. */
public class EnumDescriptor<T extends Enum<T>> extends DataTypeDescriptor<T> {
	private final List<T> values;
	private final Map<String, T> namesToValues;

	public static <T extends Enum<T>> EnumDescriptor<T> of(final Class<T> javaClass) {
		return new EnumDescriptor<T>(javaClass);
	}

	private EnumDescriptor(final Class<T> javaClass) {
		super(TypeEnum.ENUM, javaClass);

		values = ImmutableCollections.list(javaClass.getEnumConstants());
		namesToValues = ImmutableCollections.map(valuesToMap(values));
	}

	@Override
	public String toString() {
		return "EnumDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	/** Returns a list of enum values or an empty list. */
	public List<T> getValues() {
		return values;
	}

	/** Returns an enum value by its name or {@literal null}. */
	public T getValue(final String name) {
		if (name == null) {
			return null;
		}
		String uppercased = name.toUpperCase();
		return namesToValues.get(uppercased);
	}

	private static <T extends Enum<T>> Map<String, T> valuesToMap(final List<T> values) {
		Map<String, T> temp = new LinkedHashMap<String, T>();
		for (T value : values) {
			temp.put(value.name().toUpperCase(), value);
		}
		return temp;
	}
}
