package com.ivankorobkov.pdef.json;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import com.ivankorobkov.pdef.data.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LineFormatImpl implements LineFormat {

	private static final String MESSAGE_START = "{";
	private static final String MESSAGE_END = "}";
	private static final String FIELD_DELIMITER = "-";
	private static final Splitter FIELD_SPLITTER = Splitter.on(FIELD_DELIMITER);

	private static final String TRUE = "1";
	private static final String FALSE = "0";

	private static final LineFormatImpl INSTANCE = new LineFormatImpl();

	public static LineFormatImpl getInstance() {
		return INSTANCE;
	}

	@Override
	public String toJson(final Message message) {
		if (message == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		writeMessageBody(message, sb);
		return sb.toString();
	}

	@Override
	public Message fromJson(final MessageDescriptor descriptor, final String s)
			throws IOException {
		if (s == null) {
			return null;
		}

		String s1 = StringUtils.strip(s, "\"");
		if (s1 == null) {
			return null;
		}

		List<Object> tokens = parseTokens(s1);
		return readMessage(descriptor, tokens);
	}

	private Message readMessage(final MessageDescriptor descriptor, final List<Object> tokens)
			throws IOException {
		List<MessageField> fields = descriptor.getFields();
		Map<String, Object> map = Maps.newLinkedHashMap();

		for (int i = 0; i < fields.size(); i++) {
			MessageField field = fields.get(i);
			if (i == tokens.size()) {
				break;
			}

			String name = field.getName();
			Object token = tokens.get(i);
			Object value = readObject(field.getDescriptor(), token);
			map.put(name, value);
		}

		if (descriptor.isTypeBase()) {
			// It's a polymorphic message, now, we simply reread the tokens with a new descriptor.
			// TODO: Optimize rereading?

			MessageField typeBaseField = descriptor.getTypeBaseField();
			String name = typeBaseField.getName();
			Enum<?> type = (Enum<?>) map.get(name);

			MessageDescriptor subdescriptor = descriptor.getSubtypeMap().get(type);
			if (subdescriptor != null && subdescriptor != descriptor) {
				return readMessage(subdescriptor, tokens);
			}

			// TODO: Log that a subtype is not found.
		}

		// Create the message.
		Message message = descriptor.createInstance();
		for (MessageField field : fields) {
			setField(map, message, field);
		}

		return message;
	}

	private <T> void setField(final Map<String, Object> map, final Message message,
			final MessageField field) {
		if (field.isReadOnly()) {
			return;
		}

		String name = field.getName();
		Object value = map.get(name);
		if (value == null) {
			return;
		}

		// The descriptor forces the type.
		@SuppressWarnings("unchecked")
		T cast = (T) value;
		field.set(message, cast);
	}

	private Object readObject(final DataTypeDescriptor descriptor, final Object token)
			throws IOException {
		if (descriptor instanceof MessageDescriptor) {
			if (!(token instanceof List)) {
				throw new IOException("Expected a message for " + descriptor + ", got " + token);
			}

			// Can be only a list of objects
			@SuppressWarnings("unchecked")
			List<Object> list = (List) token;
			return readMessage((MessageDescriptor) descriptor, list);

		} else if (descriptor instanceof ValueDescriptor) {
			if (!(token instanceof String)) {
				throw new IOException("Expected a string for " + descriptor + ", got " + token);
			}
			return readValue((ValueDescriptor) descriptor, (String) token);

		} else if (descriptor instanceof EnumDescriptor) {
			if (!(token instanceof String)) {
				throw new IOException("Expected a string for " + descriptor + ", got " + token);
			}
			return readEnum((EnumDescriptor) descriptor, (String) token);

		} else {
			throw new IllegalArgumentException("Unsupported descriptor " + descriptor);
		}
	}

	private Object readValue(final ValueDescriptor descriptor, final String token) {
		if (token == null || token.equals("")) {
			return null;
		}

		Class<?> type = descriptor.getType();
		if (type == String.class) {
			return percentDecode(token);

		} else if (type == Boolean.class) {
			return token.equals(TRUE);

		} else if (type == Short.class) {
			long value = Long.parseLong(token);
			return Shorts.checkedCast(value);

		} else if (type == Integer.class) {
			long value = Long.parseLong(token);
			return Ints.checkedCast(value);

		} else if (type == Long.class) {
			return Long.parseLong(token);

		} else if (type == Float.class) {
			return Float.parseFloat(token);

		} else if (type == Double.class) {
			return Double.parseDouble(token);
		}

		throw new IllegalArgumentException("Unsupported value descriptor " + descriptor);
	}

	private Object readEnum(final EnumDescriptor descriptor, final String token) {
		if (token == null || token.equals("")) {
			return null;
		}

		String enumValue = token.toUpperCase();
		return descriptor.getValues().get(enumValue);
	}

	private void writeMessage(final Message message, final StringBuilder sb) {
		sb.append(MESSAGE_START);
		writeMessageBody(message, sb);
		sb.append(MESSAGE_END);
	}

	private void writeMessageBody(final Message message, final StringBuilder sb) {
		if (message == null) {
			return;
		}

		MessageDescriptor descriptor = message.getDescriptor();
		List<MessageField> fields = descriptor.getFields();

		boolean first = true;
		for (MessageField field : fields) {
			if (first) {
				first = false;
			} else {
				sb.append(FIELD_DELIMITER);
			}

			writeField(field, message, sb);
		}
	}

	private <T> void writeField(final MessageField field, final Message message,
			final StringBuilder sb) {
		if (!field.isSetIn(message)) {
			return;
		}

		DataTypeDescriptor descriptor = field.getDescriptor();
		Object value = field.get(message);
		writeObject(descriptor, value, sb);
	}

	private void writeObject(final DataTypeDescriptor descriptor, final Object object,
			final StringBuilder sb) {
		if (object == null) {
			return;
		}

		if (descriptor instanceof ValueDescriptor) {
			writeValue((ValueDescriptor) descriptor, object, sb);
		} else if (descriptor instanceof MessageDescriptor) {
			writeMessage((Message) object, sb);
		} else if (descriptor instanceof EnumDescriptor) {
			writeEnum((EnumDescriptor) descriptor, object, sb);
		} else {
			// TODO: MapDescriptor.
		}
	}

	private void writeValue(final ValueDescriptor descriptor, final Object object,
			final StringBuilder sb) {
		if (object == null) {
			return;
		}

		Class<?> type = descriptor.getType();
		if (type == String.class) {
			String value = percentEncode((String) object);
			sb.append(value);

		} else if (type == Boolean.class) {
			Boolean value = (Boolean) object;
			if (value) {
				sb.append(TRUE);
			} else {
				sb.append(FALSE);
			}

		} else if (type == Short.class) {
			String value = Short.toString((Short) object);
			sb.append(value);

		} else if (type == Integer.class) {
			String value = Integer.toString((Integer) object);
			sb.append(value);

		} else if (type == Long.class) {
			String value = Long.toString((Long) object);
			sb.append(value);

		} else if (type == Float.class) {
			String value = Float.toString((Float) object);
			sb.append(value);

		} else if (type == Double.class) {
			String value = Double.toString((Double) object);
			sb.append(value);

		} else {
			throw new IllegalArgumentException("Unsupported value type " + type);
		}
	}

	private void writeEnum(final EnumDescriptor descriptor, final Object object,
			final StringBuilder sb) {
		if (object == null) {
			return;
		}

		Enum enumValue = (Enum) object;
		String value = enumValue.name().toLowerCase();
		sb.append(value);
	}


	private static final String[][] encoding;

	static {
		encoding = new String[][]{
				{"%", ".", "-", "/", "[", "]", "{", "|", "}"},
				{"%25", "%2E", "%2D", "%2F", "%5B", "%5D", "%7B", "%7C", "%7D"}
		};
	}

	static String percentEncode(final String s) {
		return StringUtils.replaceEach(s, encoding[0], encoding[1]);
	}

	static String percentDecode(final String s) {
		return StringUtils.replaceEach(s, encoding[1], encoding[0]);
	}

	List<Object> parseTokens(final String s) throws IOException {
		Iterable<String> fields = FIELD_SPLITTER.split(s);

		List<List<Object>> stack = Lists.newArrayList();
		List<Object> message = Lists.newArrayListWithCapacity(5);

		for (String field : fields) {
			while (true) {
				// Recursively consume the same field, because it can be a start for multiple
				// messages, i.e. "{{{".
				if (field.startsWith(MESSAGE_START)) {
					field = field.substring(1, field.length());

					// And a child message to the current message fields.
					List<Object> child = Lists.newArrayListWithCapacity(5);
					message.add(child);

					// Push the current message on the stack and replace it with the child.
					stack.add(message);
					message = child;

				} else if (field.endsWith(MESSAGE_END)) {
					field = field.substring(0, field.length() - 1);
					message.add(field);

					// Pop the message from the stack, or return, if the stack is empty.
					if (!stack.isEmpty()) {
						message = stack.remove(stack.size() - 1);
						break;
					}

					// Malformed string.
					throw new IOException("Malformed string \"" + s +"\"");

				} else {
					message.add(field);
					break;
				}
			}
		}

		return message;
	}
}
