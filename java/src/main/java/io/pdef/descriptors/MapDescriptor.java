package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.Map;

public class MapDescriptor<K, V> extends ValueDescriptor<Map<K, V>> {
	private final ValueDescriptor<K> key;
	private final ValueDescriptor<V> value;

	@SuppressWarnings("unchecked")
	MapDescriptor(final ValueDescriptor<K> key, final ValueDescriptor<V> value) {
		super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
		if (key == null) throw new NullPointerException("key");
		if (value == null) throw new NullPointerException("value");

		this.key = key;
		this.value = value;
	}

	/** Returns a map key descriptor. */
	public ValueDescriptor<K> getKey() {
		return key;
	}

	/** Returns a map value descriptor. */
	public ValueDescriptor<V> getValue() {
		return value;
	}
}
