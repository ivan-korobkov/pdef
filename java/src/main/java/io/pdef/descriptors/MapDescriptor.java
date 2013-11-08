package io.pdef.descriptors;

import java.util.Map;

public interface MapDescriptor<K, V> extends ValueDescriptor<Map<K, V>> {
	/**
	 * Returns a map key descriptor.
	 */
	ValueDescriptor<K> getKey();

	/**
	 * Returns a map value descriptor.
	 */
	ValueDescriptor<V> getValue();
}
