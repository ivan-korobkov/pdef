package com.ivankorobkov.pdef.data;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;
import com.ivankorobkov.pdef.Pdef;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public class MapDescriptor<K, V> extends AbstractDataTypeDescriptor {

	public static MapDescriptor<?, ?> getInstance() {
		return BuiltinPackage.getInstance().getMap();
	}

	private final TypeToken<K> keyType;
	private final TypeToken<V> valueType;
	private DataTypeDescriptor key;
	private DataTypeDescriptor value;

	MapDescriptor() {
		this(new TypeToken<Map<K, V>>() {}, Pdef.classVariablesAsMap(MapDescriptor.class));
	}

	private MapDescriptor(
			final TypeToken<?> type,
			final Map<TypeVariable<?>, TypeToken<?>> args) {
		super(type, args);

		this.keyType = Pdef.parameterizeTypeUnchecked(new TypeToken<K>(getClass()) {}, args);
		this.valueType = Pdef.parameterizeTypeUnchecked(new TypeToken<V>(getClass()) {}, args);
	}

	@Override
	public String getName() {
		return "Map";
	}

	public TypeToken<K> getKeyType() {
		return keyType;
	}

	public TypeToken<V> getValueType() {
		return valueType;
	}

	public DataTypeDescriptor getKey() {
		return key;
	}

	public DataTypeDescriptor getValue() {
		return value;
	}

	@Override
	protected MapDescriptor parameterize(final TypeToken<?> ptoken,
			final Map<TypeVariable<?>, TypeToken<?>> args) {
		return new MapDescriptor<K, V>(ptoken, args);
	}

	@Override
	public void link(final DescriptorPool pool) {
		key = pool.get(keyType);
		value = pool.get(valueType);
	}

	@Override
	public Object merge(@Nullable final Object object, @Nullable final Object another) {
		if (another == null) {
			return object;
		}

		Map<?, ?> map = (Map<?, ?>) another;
		Map<Object, Object> merged = Maps.newLinkedHashMap();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			Object key = e.getKey();
			Object val = e.getValue();
			Object copiedKey = this.key.deepCopy(key);
			Object copiedVal = this.value.deepCopy(val);
			merged.put(copiedKey, copiedVal);
		}

		return merged;
	}

	@Override
	public Object deepCopy(@Nullable final Object object) {
		if (object == null) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) object;
		return Maps.newLinkedHashMap(map);
	}
}
