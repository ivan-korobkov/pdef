package pdef.formats;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.pdef.SerializationException;
import io.pdef.SerializationException;
import org.apache.commons.lang3.StringUtils;
import pdef.*;
import pdef.rpc.Call;

import java.util.List;

public class StringParser extends AbstractParser {
	static final Splitter FIELD_SPLITTER = Splitter.on(StringSerializer.FIELD_DELIMITER);

	@Override
	public Object parse(final TypeDescriptor descriptor, final Object object) {
		String s = (String) object;
		List<Object> tokens = parseTokens(s);
		return parseMessage((MessageDescriptor) descriptor, tokens);
	}

	@Override
	public List<Call> parseCalls(final InterfaceDescriptor descriptor, final Object object) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object parseMessage(final MessageDescriptor descriptor, final Object object) {
		if ("".equals(object)) return null;
		List<?> tokens = (List<?>) object;
		if (tokens.isEmpty()) return null;

		MessageDescriptor polymorphic = parseDescriptorType(descriptor, tokens);
		List<FieldDescriptor> fields = polymorphic.getFields().list();

		Message.Builder builder = polymorphic.newBuilder();
		for (int i = 0; i < fields.size(); i++) {
			FieldDescriptor field = fields.get(i);
			if (i == tokens.size()) break;

			Object token = tokens.get(i);
			Object value = doParse(field.getType(), token);
			// Even-though the field is read-only we still parse it to validate the data.
			if (field.isTypeField()) continue;
			field.set(builder, value);
		}

		return builder.build();
	}

	private MessageDescriptor parseDescriptorType(final MessageDescriptor descriptor,
			final List<?> tokens) {
		Subtypes tree = descriptor.getSubtypes();
		if (tree == null) return descriptor;

		Object typeToken = null;
		FieldDescriptor typeField = tree.getField();
		List<FieldDescriptor> fields = descriptor.getFields().list();
		for (int i = 0; i < tokens.size(); i++) {
			typeToken = tokens.get(i);

			if (i == fields.size()) break;
			if (fields.get(i) == typeField) break;
		}

		if (typeToken == null) return descriptor;
		Object type = doParse(typeField.getType(), typeToken);
		MessageDescriptor subd = tree.getMap().get(type);

		// TODO: Log if a subtype is not found.
		if (subd == null || subd == descriptor) return descriptor;
		return parseDescriptorType(subd, tokens);
	}

	@Override
	protected Object parseEnum(final EnumDescriptor descriptor, final Object object) {
		String s = ((String) object).toUpperCase();
		return "".equals(s) ? null : descriptor.getValues().get((s));
	}

	@Override
	protected Object parseList(final ListDescriptor descriptor, final Object object) {
		if ("".equals(object)) return null;
		throw new SerializationException("StringParser does not support lists, got " + object);
	}

	@Override
	protected Object parseSet(final SetDescriptor descriptor, final Object object) {
		if ("".equals(object)) return null;
		throw new SerializationException("StringParser does not support sets, got " + object);
	}

	@Override
	protected Object parseMap(final MapDescriptor descriptor, final Object object) {
		if ("".equals(object)) return null;
		throw new SerializationException("StringParser does not support maps, got " + object);
	}

	@Override
	protected Boolean parseBoolean(final Object value) {
		if (StringSerializer.TRUE.equals(value)) return true;
		if (StringSerializer.FALSE.equals(value)) return false;
		if ("".equals(value)) return false;
		throw new SerializationException("Failed to parse a boolean from " + value);
	}

	@Override
	protected Short parseShort(final Object value) {
		return "".equals(value) ? 0 : Short.parseShort((String) value);
	}

	@Override
	protected Integer parseInt(final Object value) {
		return "".equals(value) ? 0 : Integer.parseInt((String) value);
	}

	@Override
	protected Long parseLong(final Object value) {
		return "".equals(value) ? 0 : Long.parseLong((String) value);
	}

	@Override
	protected Float parseFloat(final Object value) {
		return "".equals(value) ? 0 : Float.parseFloat((String) value);
	}

	@Override
	protected Double parseDouble(final Object value) {
		return "".equals(value) ? 0 : Double.parseDouble((String) value);
	}

	@Override
	protected String parseString(final Object value) {
		if ("".equals(value)) return null;
		String s = (String) value;
		return StringParser.percentDecode(s);
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

	List<Object> parseTokens(final String s) {
		Iterable<String> fields = FIELD_SPLITTER.split(s);

		List<List<Object>> stack = Lists.newArrayList();
		List<Object> message = Lists.newArrayListWithCapacity(5);

		for (String field : fields) {
			while (true) {
				// Recursively consume the same field, because it can be a start for multiple
				// messages, i.e. "{{{".
				if (field.startsWith(StringSerializer.MESSAGE_START)) {
					field = field.substring(1, field.length());

					// And a child message to the current message fields.
					List<Object> child = Lists.newArrayListWithCapacity(5);
					message.add(child);

					// Push the current message on the stack and replace it with the child.
					stack.add(message);
					message = child;

				} else if (field.endsWith(StringSerializer.MESSAGE_END)) {
					field = field.substring(0, field.length() - 1);
					message.add(field);

					// Pop the message from the stack, or return, if the stack is empty.
					if (!stack.isEmpty()) {
						message = stack.remove(stack.size() - 1);
						break;
					}

					// Malformed string.
					throw new SerializationException("Malformed string \"" + s +"\"");

				} else {
					message.add(field);
					break;
				}
			}
		}

		return message;
	}
}
