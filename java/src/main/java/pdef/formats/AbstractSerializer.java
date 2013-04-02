package pdef.formats;

import pdef.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSerializer implements Serializer {
	@Override
	public Object serialize(final Message message) {
		if (message == null) return null;
		MessageDescriptor descriptor = message.getDescriptor();
		return serialize(descriptor, message);
	}

	protected Object serialize(TypeDescriptor descriptor, Object object) {
		if (descriptor instanceof ValueDescriptor) {
			return serializeValue((ValueDescriptor) descriptor, object);
		} else if (descriptor instanceof MessageDescriptor) {
			return serializeMessage((MessageDescriptor) descriptor, (Message) object);
		} else if (descriptor instanceof EnumDescriptor) {
			return serializeEnum((EnumDescriptor) descriptor, (Enum<?>) object);
		} else if (descriptor instanceof ListDescriptor) {
			return serializeList((ListDescriptor) descriptor, (List<?>) object);
		} else if (descriptor instanceof SetDescriptor) {
			return serializeSet((SetDescriptor) descriptor, (Set<?>) object);
		} else if (descriptor instanceof MapDescriptor) {
			return serializeMap((MapDescriptor) descriptor, (Map <?, ?>) object);
		}

		throw new FormatException("Unsupported descriptor " + descriptor + ", object " + object);
	}

	protected abstract Object serializeMessage(final MessageDescriptor descriptor,
			final Message object);

	protected abstract Object serializeEnum(final EnumDescriptor descriptor, final Enum<?> object);

	protected abstract Object serializeList(final ListDescriptor descriptor, final List<?> object);

	protected abstract Object serializeSet(final SetDescriptor descriptor, final Set<?> object);

	protected abstract Object serializeMap(final MapDescriptor descriptor, final Map<?, ?> object);

	protected Object serializeValue(final ValueDescriptor descriptor,
			final Object value) {
		Class<?> cls = descriptor.getJavaClass();
		if (cls == boolean.class) {
			return serializeBoolean((Boolean) value);
		} else if (cls == short.class) {
			return serializeShort((Short) value);
		} else if (cls == int.class) {
			return serializeInt((Integer) value);
		} else if (cls == long.class) {
			return serializeLong((Long) value);
		} else if (cls == float.class) {
			return serializeFloat((Float) value);
		} else if (cls == double.class) {
			return serializeDouble((Double) value);
		} else if (cls == String.class) {
			return serializeString((String) value);
		} else if (cls == void.class) {
			return serializeVoid();
		} else if (cls == Object.class) {
			return serializeObject(value);
		}

		throw new FormatException("Unsupported value class " + cls + ", value " + value);
	}

	protected abstract Object serializeBoolean(final Boolean value);

	protected abstract Object serializeShort(final Short value);

	protected abstract Object serializeInt(final Integer value);

	protected abstract Object serializeLong(final Long value);

	protected abstract Object serializeFloat(final Float value);

	protected abstract Object serializeDouble(final Double value);

	protected abstract Object serializeString(final String value);

	protected Object serializeVoid() { return null; }

	protected Object serializeObject(final Object value) { return value; }

	protected MessageDescriptor getDescriptorForType(final MessageDescriptor descriptor,
			final Message message) {
		Subtypes tree = descriptor.getSubtypes();
		if (tree == null) return descriptor;
		return message.getDescriptor();
	}
}
