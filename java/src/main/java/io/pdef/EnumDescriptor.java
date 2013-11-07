package io.pdef;

import javax.annotation.Nullable;
import java.util.List;

public interface EnumDescriptor<T extends Enum<T>> extends DataTypeDescriptor<T> {
	/**
	 * Returns an enum value by its name or {@literal null}.
	 */
	@Nullable
	T getValue(String name);

	/**
	 * Returns a list of enum values or an empty list.
	 */
	List<T> getValues();
}
