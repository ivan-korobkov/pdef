package io.pdef.meta;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * EnumType holds enum values and parsing/serialization methods.
 * */
public class EnumType<T extends Enum<T>> extends DataType<T> {
	private final Class<T> javaClass;
	private final List<T> values;
	private final Map<String, T> namesToValues;

	public static <T extends Enum<T>> EnumType<T> of(final Class<T> javaClass) {
		return new EnumType<T>(javaClass);
	}

	private EnumType(final Class<T> javaClass) {
		super(TypeEnum.ENUM);

		this.javaClass = checkNotNull(javaClass);
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
				.addValue(javaClass.getSimpleName())
				.toString();
	}

	/** Returns a java class. */
	public Class<T> getJavaClass() {
		return javaClass;
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
