package pdef.formats;

import pdef.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StringSerializer extends AbstractSerializer {
	static final String TRUE = "1";
	static final String FALSE = "0";
	static final String FIELD_DELIMITER = "-";
	static final String MESSAGE_START = "{";
	static final String MESSAGE_END = "}";

	@Override
	public Object serialize(final Message message) {
		String s = (String) super.serialize(message);
		if (s.startsWith("{") && s.endsWith("}")) return s.substring(1, s.length() - 1);
		return s;
	}

	@Override
	protected String serializeMessage(final MessageDescriptor descriptor, final Message message) {
		if (message == null) return "";
		MessageDescriptor polymorphicOrParameterized = getDescriptorForType(descriptor, message);
		SymbolTable<FieldDescriptor> fields = polymorphicOrParameterized.getFields();

		boolean first = true;
		StringBuilder sb = new StringBuilder();
		sb.append(MESSAGE_START);
		for (FieldDescriptor field : fields) {
			if (first) first = false; else sb.append(FIELD_DELIMITER);
			if (!field.isSet(message)) continue;

			Object value = field.get(message);
			TypeDescriptor type = field.getType();
			String s = (String) serialize(type, value);
			sb.append(s);
		}
		sb.append(MESSAGE_END);
		return sb.toString();
	}

	@Override
	protected String serializeEnum(final EnumDescriptor descriptor, final Enum<?> object) {
		return object == null ? "" : object.name().toLowerCase();
	}

	@Override
	protected String serializeList(final ListDescriptor descriptor, final List<?> object) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String serializeSet(final SetDescriptor descriptor, final Set<?> object) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String serializeMap(final MapDescriptor descriptor, final Map<?, ?> object) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String serializeBoolean(final Boolean value) {
		return value == null ? "" : value ? TRUE : FALSE;
	}

	@Override
	protected String serializeShort(final Short value) {
		return value == null ? "" : value.toString();
	}

	@Override
	protected String serializeInt(final Integer value) {
		return value == null ? "" : value.toString();
	}

	@Override
	protected String serializeLong(final Long value) {
		return value == null ? "" : value.toString();
	}

	@Override
	protected String serializeFloat(final Float value) {
		return value == null ? "" : value.toString();
	}

	@Override
	protected String serializeDouble(final Double value) {
		return value == null ? "" : value.toString();
	}

	@Override
	protected String serializeString(final String value) {
		if (value == null) return "";
		return StringParser.percentEncode(value);
	}
}
