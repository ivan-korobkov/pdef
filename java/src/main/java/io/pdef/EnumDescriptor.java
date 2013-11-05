package io.pdef;

import java.util.List;
import java.util.Map;

public interface EnumDescriptor<T extends Enum<T>> extends DataDescriptor<T> {
	/**
	 * Returns a list of enum values or an empty list.
	 */
	List<T> getValues();

	/**
	 * Returns a map of uppercase names to enum values or an empty map.
	 */
	Map<String, T> getNamesToValues();
}
