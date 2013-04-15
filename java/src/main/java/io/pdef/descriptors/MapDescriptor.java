package io.pdef.descriptors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MapDescriptor extends AbstractDescriptor {
	private final Type keyType;
	private final Type valueType;
	private Descriptor key;
	private Descriptor value;

	public MapDescriptor(final ParameterizedType mapType, final DescriptorPool pool) {
		super(mapType, DescriptorType.MAP, pool);

		Type[] args = mapType.getActualTypeArguments();
		keyType = args[0];
		valueType = args[1];
	}

	public Type getKeyType() {
		return keyType;
	}

	public Type getValueType() {
		return valueType;
	}

	public Descriptor getKey() {
		return key;
	}

	public Descriptor getValue() {
		return value;
	}

	@Override
	protected void doLink() {
		key = pool.getDescriptor(keyType);
		value = pool.getDescriptor(valueType);
	}
}
