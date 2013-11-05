package io.pdef;

import java.util.Map;

public interface MapDescriptor<K, V> extends DataDescriptor<Map<K, V>> {
	/**
	 * Returns a map key descriptor.
	 */
	DataDescriptor<K> getKey();

	/**
	 * Returns a map value descriptor.
	 */
	DataDescriptor<V> getValue();
}
