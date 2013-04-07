package pdef.formats;

import pdef.*;

public abstract class AbstractParser implements Parser {
	@Override
	public Object parse(final TypeDescriptor descriptor, final Object object) {
		if (object == null) return null;
		return doParse(descriptor, object);
	}

	protected Object doParse(TypeDescriptor descriptor, Object object) {
		if (descriptor instanceof ValueDescriptor) {
			return parseValue((ValueDescriptor) descriptor, object);
		} else if (descriptor instanceof MessageDescriptor) {
			return parseMessage((MessageDescriptor) descriptor, object);
		} else if (descriptor instanceof EnumDescriptor) {
			return parseEnum((EnumDescriptor) descriptor, object);
		} else if (descriptor instanceof ListDescriptor) {
			return parseList((ListDescriptor) descriptor, object);
		} else if (descriptor instanceof SetDescriptor) {
			return parseSet((SetDescriptor) descriptor, object);
		} else if (descriptor instanceof MapDescriptor) {
			return parseMap((MapDescriptor) descriptor, object);
		}
		throw new FormatException("Unsupported descriptor " + descriptor + ", object " + object);
	}

	protected abstract Object parseMessage(final MessageDescriptor descriptor, final Object object);

	protected abstract Object parseEnum(final EnumDescriptor descriptor, final Object object);

	protected abstract Object parseList(final ListDescriptor descriptor, final Object object);

	protected abstract Object parseSet(final SetDescriptor descriptor, final Object object);

	protected abstract Object parseMap(final MapDescriptor descriptor, final Object object);

	protected Object parseValue(final ValueDescriptor descriptor, final Object value) {
		Class<?> cls = descriptor.getJavaClass();
		if (cls == boolean.class) {
			return parseBoolean(value);
		} else if (cls == short.class) {
			return parseShort(value);
		} else if (cls == int.class) {
			return parseInt(value);
		} else if (cls == long.class) {
			return parseLong(value);
		} else if (cls == float.class) {
			return parseFloat(value);
		} else if (cls == double.class) {
			return parseDouble(value);
		} else if (cls == String.class) {
			return parseString(value);
		} else if (cls == void.class) {
			return parseVoid();
		} else if (cls == Object.class) {
			return parseObject(value);
		}

		throw new FormatException("Unsupported value class " + cls + ", value " + value);
	}

	protected abstract Boolean parseBoolean(final Object value);

	protected abstract Short parseShort(final Object value);

	protected abstract Integer parseInt(final Object value);

	protected abstract Long parseLong(final Object value);

	protected abstract Float parseFloat(final Object value);

	protected abstract Double parseDouble(final Object value);

	protected abstract String parseString(final Object value);

	protected Object parseVoid() { return null; }

	protected Object parseObject(final Object value) { return value; }
}
