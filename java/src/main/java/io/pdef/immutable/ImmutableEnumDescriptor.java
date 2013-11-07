package io.pdef.immutable;

import io.pdef.BaseDescriptor;
import io.pdef.DataTypeDescriptor;
import io.pdef.EnumDescriptor;
import io.pdef.TypeEnum;

import java.util.*;

/** EnumDescriptor holds enum values and parsing/serialization methods. */
public class ImmutableEnumDescriptor<T extends Enum<T>> extends BaseDescriptor<T>
		implements EnumDescriptor<T>,DataTypeDescriptor<T> {
	private final List<T> values;
	private final Map<String, T> namesToValues;

	public static <T extends Enum<T>> ImmutableEnumDescriptor<T> of(final Class<T> javaClass) {
		return new ImmutableEnumDescriptor<T>(javaClass);
	}

	private ImmutableEnumDescriptor(final Class<T> javaClass) {
		super(TypeEnum.ENUM, javaClass);
		values = ImmutableCollections.list(javaClass.getEnumConstants());
		namesToValues = ImmutableCollections.map(valuesToMap(values));
	}

	@Override
	public String toString() {
		return "EnumDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	@Override
	public List<T> getValues() {
		return values;
	}

	@Override
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
