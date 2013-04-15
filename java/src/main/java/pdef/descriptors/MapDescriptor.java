package pdef.descriptors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MapDescriptor extends AbstractDescriptor {
	private final Type keyType;
	private final Type valueType;
	private Descriptor key;
	private Descriptor element;

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

	public Descriptor getElement() {
		return element;
	}

	@Override
	protected void doLink() {
		key = pool.getDescriptor(keyType);
		element = pool.getDescriptor(valueType);
	}
}
