package io.pdef;

import java.util.Map;

public interface MapDescriptor<K, V> extends DataTypeDescriptor<Map<K, V>> {
	/**
	 * Returns a map key descriptor.
	 */
	DataTypeDescriptor<K> getKey();

	/**
	 * Returns a map value descriptor.
	 */
	DataTypeDescriptor<V> getValue();
}
