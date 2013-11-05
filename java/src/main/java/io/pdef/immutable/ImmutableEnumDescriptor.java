package io.pdef.immutable;

import io.pdef.AbstractDataDescriptor;
import io.pdef.EnumDescriptor;
import io.pdef.TypeEnum;

import java.util.*;

/** EnumDescriptor holds enum values and parsing/serialization methods. */
public class ImmutableEnumDescriptor<T extends Enum<T>> extends AbstractDataDescriptor<T>
		implements EnumDescriptor<T> {
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
	public Map<String, T> getNamesToValues() {
		return namesToValues;
	}

	@Override
	public T copy(final T object) {
		return object;
	}

	private static <T extends Enum<T>> Map<String, T> valuesToMap(final List<T> values) {
		Map<String, T> temp = new LinkedHashMap<String, T>();
		for (T value : values) {
			temp.put(value.name().toUpperCase(), value);
		}
		return temp;
	}
}
