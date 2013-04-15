package io.pdef;

import io.pdef.descriptors.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSerializer implements Serializer {
	protected final DescriptorPool pool;

	public AbstractSerializer(final DescriptorPool pool) {
		this.pool = pool;
	}

	@Override
	public Object serialize(final Object object) {
		if (object == null) return null;

		Descriptor descriptor = pool.getDescriptor(object.getClass());
		return serialize(descriptor, object);
	}

	protected Object serialize(final Descriptor descriptor, final Object object) {
		switch (descriptor.getType()) {
			case MESSAGE: return serializeMessage((MessageDescriptor) descriptor, (Message) object);
			case ENUM: return serializeEnum((EnumDescriptor) descriptor, (Enum<?>) object);
			case LIST: return serializeList((ListDescriptor) descriptor, (List<?>) object);
			case MAP: return serializeMap((MapDescriptor) descriptor, (Map<?, ?>) object);
			case SET: return serializeSet((SetDescriptor) descriptor, (Set<?>) object);
			case VALUE: return serializeValue((ValueDescriptor) descriptor, object);
			// case INTERFACE is not supported
		}

		throw new SerializationException("Unsupported descriptor " + descriptor + ", object " + object);
	}

	protected Object serializeValue(final ValueDescriptor descriptor, final Object value) {
		Class<?> cls = descriptor.getJavaType();
		if (cls == boolean.class) return serializeBoolean((Boolean) value);
		else if (cls == short.class) return serializeShort((Short) value);
		else if (cls == int.class) return serializeInt((Integer) value);
		else if (cls == long.class) return serializeLong((Long) value);
		else if (cls == float.class) return serializeFloat((Float) value);
		else if (cls == double.class) return serializeDouble((Double) value);
		else if (cls == String.class) return serializeString((String) value);
		else if (cls == Object.class) return serializeObject(value);
		else if (cls == void.class) return serializeVoid();

		throw new SerializationException("Unsupported value class " + cls + ", value " + value);
	}

	protected Object serializeObject(final Object value) { return value; }

	protected Object serializeVoid() { return null; }

	protected abstract Object serializeMessage(final MessageDescriptor descriptor,
			final Message object);

	protected abstract Object serializeEnum(final EnumDescriptor descriptor, final Enum<?> object);

	protected abstract Object serializeList(final ListDescriptor descriptor, final List<?> object);

	protected abstract Object serializeMap(final MapDescriptor descriptor, final Map<?, ?> object);

	protected abstract Object serializeSet(final SetDescriptor descriptor, final Set<?> object);

	protected abstract Object serializeBoolean(final Boolean value);

	protected abstract Object serializeShort(final Short value);

	protected abstract Object serializeInt(final Integer value);

	protected abstract Object serializeLong(final Long value);

	protected abstract Object serializeFloat(final Float value);

	protected abstract Object serializeDouble(final Double value);

	protected abstract Object serializeString(final String value);
}
