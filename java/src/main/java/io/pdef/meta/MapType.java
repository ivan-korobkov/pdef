package io.pdef.meta;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Maps;

import java.util.Map;

public class MapType<K, V> extends DataType<Map<K, V>> {
	private final DataType<K> key;
	private final DataType<V> value;

	public MapType(final DataType<K> key, final DataType<V> value) {
		super(TypeEnum.MAP);
		this.key = checkNotNull(key);
		this.value = checkNotNull(value);
	}

	public DataType<K> getKey() {
		return key;
	}

	public DataType<V> getValue() {
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
