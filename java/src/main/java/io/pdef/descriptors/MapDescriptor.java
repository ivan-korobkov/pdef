package io.pdef.descriptors;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapDescriptor<K, V> extends DataDescriptor<Map<K, V>> {
	private final DataDescriptor<K> key;
	private final DataDescriptor<V> value;

	@SuppressWarnings("unchecked")
	MapDescriptor(final DataDescriptor<K> key, final DataDescriptor<V> value) {
		super(TypeEnum.MAP, (Class<Map<K, V>>) (Class<?>) Map.class);
		this.key = key;
		this.value = value;
		if (key == null) throw new NullPointerException("key");
		if (value == null) throw new NullPointerException("value");
	}

	public DataDescriptor<K> getKey() {
		return key;
	}

	public DataDescriptor<V> getValue() {
		return value;
	}

	@Override
	public Map<K, V> copy(final Map<K, V> map) {
		if (map == null) {
			return null;
		}

		Map<K, V> copy = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			K ck = key.copy(entry.getKey());
			V cv = value.copy(entry.getValue());
			copy.put(ck, cv);
		}

		return copy;
	}
}
