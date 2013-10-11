package io.pdef.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Maps;

import java.util.Map;

public class MapDescriptor<K, V> extends DataDescriptor<Map<K, V>> {
	private final DataDescriptor<K> key;
	private final DataDescriptor<V> value;

	public MapDescriptor(final DataDescriptor<K> key, final DataDescriptor<V> value) {
		super(TypeEnum.MAP);
		this.key = checkNotNull(key);
		this.value = checkNotNull(value);
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

		Map<K, V> copy = Maps.newHashMap();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			K ck = key.copy(entry.getKey());
			V cv = value.copy(entry.getValue());
			copy.put(ck, cv);
		}

		return copy;
	}
}
