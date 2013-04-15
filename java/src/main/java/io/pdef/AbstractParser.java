package io.pdef;

import io.pdef.descriptors.*;
import pdef.formats.FormatException;

import java.lang.reflect.Type;

public abstract class AbstractParser implements Parser {
	protected final DescriptorPool pool;

	public AbstractParser(final DescriptorPool pool) {
		this.pool = pool;
	}

	@Override
	public Object parse(final Type type, final Object object) {
		if (object == null) return null;
		Descriptor descriptor = pool.getDescriptor(type);
		return doParse(descriptor, object);
	}

	protected Object doParse(Descriptor descriptor, Object object) {
		switch (descriptor.getType()) {
			case MESSAGE: return parseMessage((MessageDescriptor) descriptor, object);
			case ENUM: return parseEnum((EnumDescriptor) descriptor, object);
			case LIST: return parseList((ListDescriptor) descriptor, object);
			case MAP: return parseMap((MapDescriptor) descriptor, object);
			case SET: return parseSet((SetDescriptor) descriptor, object);
			case VALUE: return parseValue((ValueDescriptor) descriptor, object);
			// case INTERFACE is not supported
		}

		throw new FormatException("Unsupported descriptor " + descriptor + ", object " + object);
	}

	protected Object parseValue(final ValueDescriptor descriptor, final Object value) {
		Class<?> cls = descriptor.getJavaType();
		if (cls == boolean.class) return parseBoolean(value);
		else if (cls == short.class) return parseShort(value);
		else if (cls == int.class) return parseInt(value);
		else if (cls == long.class) return parseLong(value);
		else if (cls == float.class) return parseFloat(value);
		else if (cls == double.class) return parseDouble(value);
		else if (cls == String.class) return parseString(value);
		else if (cls == void.class) return parseVoid();
		else if (cls == Object.class) return parseObject(value);
		throw new FormatException("Unsupported value class " + cls + ", value " + value);
	}

	protected Object parseVoid() { return null; }

	protected Object parseObject(final Object value) { return value; }

	protected abstract Object parseMessage(final MessageDescriptor descriptor, final Object object);

	protected abstract Object parseEnum(final EnumDescriptor descriptor, final Object object);

	protected abstract Object parseList(final ListDescriptor descriptor, final Object object);

	protected abstract Object parseSet(final SetDescriptor descriptor, final Object object);

	protected abstract Object parseMap(final MapDescriptor descriptor, final Object object);

	protected abstract Boolean parseBoolean(final Object value);

	protected abstract Short parseShort(final Object value);

	protected abstract Integer parseInt(final Object value);

	protected abstract Long parseLong(final Object value);

	protected abstract Float parseFloat(final Object value);

	protected abstract Double parseDouble(final Object value);

	protected abstract String parseString(final Object value);
}
