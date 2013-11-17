package io.pdef.descriptors;

import io.pdef.TypeEnum;

import java.util.Map;

public class MapDescriptor<K, V> extends DataTypeDescriptor<Map<K, V>> {
	private final DataTypeDescriptor<K> key;
	private final DataTypeDescriptor<V> value;

	@SuppressWarnings("unchecked")
	MapDescriptor(final DataTypeDescriptor<K> key, final DataTypeDescriptor<V> value) {
		super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
		if (key == null) throw new NullPointerException("key");
		if (value == null) throw new NullPointerException("value");

		this.key = key;
		this.value = value;
	}

	/** Returns a map key descriptor. */
	public DataTypeDescriptor<K> getKey() {
		return key;
	}

	/** Returns a map value descriptor. */
	public DataTypeDescriptor<V> getValue() {
		return value;
	}
}
