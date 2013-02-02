package com.ivankorobkov.pdef.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.ivankorobkov.pdef.data.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonFormatImpl implements JsonFormat {

	private final ObjectMapper mapper;
	private final LineFormat lineFormat;
	private final JsonFactory factory;

	@Inject
	JsonFormatImpl(final ObjectMapper mapper, final LineFormat lineFormat) {
		this.mapper = checkNotNull(mapper);
		this.lineFormat = checkNotNull(lineFormat);
		factory = mapper.getJsonFactory();
	}

	@Override
	public void serialize(final Message value, final JsonGenerator jgen) throws IOException {
		MessageDescriptor descriptor = value.getDescriptor();
		writeObject(jgen, descriptor, value);
	}

	@Override
	public Message deserialize(final MessageDescriptor descriptor, final JsonParser parser)
			throws IOException {
		JsonNode root = parser.readValueAsTree();
		return (Message) fromJson(descriptor, root);
	}

	@Override
	public String toJson(final Message message) throws IOException {
		checkNotNull(message);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		JsonGenerator gen = factory.createJsonGenerator(stream, JsonEncoding.UTF8);
		MessageDescriptor descriptor = message.getDescriptor();
		writeObject(gen, descriptor, message);
		gen.flush();
		byte[] array = stream.toByteArray();
		return new String(array, Charsets.UTF_8);
	}

	@Override
	public Object fromJson(final MessageDescriptor descriptor, final String s) throws IOException {
		checkNotNull(descriptor);
		checkNotNull(s);

		JsonNode root;
		if (descriptor.isLineFormat()) {
			return lineFormat.fromJson(descriptor, s);
		} else {
			root = mapper.readTree(s);
		}

		return fromJson(descriptor, root);
	}

	@Override
	public Object fromJson(final MessageDescriptor descriptor, final JsonNode root)
			throws IOException {
		checkNotNull(descriptor);
		checkNotNull(root);

		Parser parser = new Parser();
		return parser.parse(descriptor, root);
	}

	private void writeObject(final JsonGenerator gen, DataTypeDescriptor descriptor,
			final Object value) throws IOException {
		if (value instanceof Message) {
			descriptor = ((Message) value).getDescriptor();
		}
		
		if (descriptor instanceof ValueDescriptor) {
			writeValue(gen, value);

		} else if (descriptor instanceof MessageDescriptor) {
			writeMessage(gen, (Message) value);

		} else if (descriptor instanceof EnumDescriptor) {
			writeEnum(gen, (EnumDescriptor) descriptor, value);

		} else if (descriptor instanceof ListDescriptor) {
			writeList(gen, (ListDescriptor) descriptor, value);

		} else if (descriptor instanceof SetDescriptor) {
			writeSet(gen, (SetDescriptor) descriptor, value);

		} else if (descriptor instanceof MapDescriptor) {
			writeMap(gen, (MapDescriptor) descriptor, value);
		}
	}

	private void writeValue(final JsonGenerator gen, final Object value) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		if (value instanceof String) {
			gen.writeString((String) value);

		} else if (value instanceof Boolean) {
			gen.writeBoolean((Boolean) value);

		} else if (value instanceof Short) {
			gen.writeNumber((int) (Short) value);

		} else if (value instanceof Integer) {
			gen.writeNumber((Integer) value);

		} else if (value instanceof Long) {
			gen.writeNumber((Long) value);

		} else if (value instanceof Float) {
			gen.writeNumber((Float) value);

		} else if (value instanceof Double) {
			gen.writeNumber((Double) value);
		}
	}

	private void writeMessage(final JsonGenerator gen, final Message message) throws IOException {
		if (message == null) {
			gen.writeNull();
			return;
		}

		MessageDescriptor descriptor = message.getDescriptor();
		if (descriptor.isLineFormat()) {
			String s = lineFormat.toJson(message);
			gen.writeString(s);
			return;
		}
		List<MessageField> fields = descriptor.getFields();

		gen.writeStartObject();
		for (MessageField field : fields) {
			if (!field.isSetIn(message)) {
				continue;
			}

			String fname = field.getName();
			Object fval = field.get(message);
			if (fval == null) {
				continue;
			}

			DataTypeDescriptor fdescr = field.getDescriptor();
			gen.writeFieldName(fname);
			writeObject(gen, fdescr, fval);
		}
		gen.writeEndObject();
	}

	private void writeEnum(final JsonGenerator gen, final EnumDescriptor descriptor,
			final Object value) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		Enum e = (Enum) value;
		String s = e.toString().toLowerCase();
		gen.writeString(s);
	}

	private void writeList(final JsonGenerator gen, final ListDescriptor descriptor,
			final Object value) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		List<?> list = (List<?>) value;
		DataTypeDescriptor elementDescriptor = descriptor.getElement();

		gen.writeStartArray();
		for (Object element : list) {
			writeObject(gen, elementDescriptor, element);
		}
		gen.writeEndArray();
	}

	private void writeSet(final JsonGenerator gen, final SetDescriptor descriptor,
			final Object value) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		Set<?> set = (Set<?>) value;
		DataTypeDescriptor elementDescriptor = descriptor.getElement();

		gen.writeStartArray();
		for (Object element : set) {
			writeObject(gen, elementDescriptor, element);
		}
		gen.writeEndArray();
	}

	private void writeMap(final JsonGenerator gen, final MapDescriptor descriptor,
			final Object value) throws IOException {
		if (value == null) {
			gen.writeNull();
			return;
		}

		Map<?, ?> map = (Map<?, ?>) value;
		DataTypeDescriptor keyDescriptor = descriptor.getKey();
		DataTypeDescriptor valueDescriptor = descriptor.getValue();
		if (keyDescriptor instanceof MessageDescriptor) {
			if (!((MessageDescriptor) keyDescriptor).isLineFormat()) {
				throw new IllegalArgumentException(
						"Only line format messages are supported as map keys, got " + keyDescriptor);
			}
		}

		gen.writeStartObject();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			Object key = e.getKey();
			Object val = e.getValue();
			String fieldName;
			if (keyDescriptor instanceof MessageDescriptor) {
				fieldName = lineFormat.toJson((Message) key);
			} else {
				fieldName = String.valueOf(key);
			}

			gen.writeFieldName(fieldName);
			writeObject(gen, valueDescriptor, val);
		}
		gen.writeEndObject();
	}

	private static final Joiner COMMA_JOINER = Joiner.on(",\n");
	private static final Joiner DOT_JOINER = Joiner.on(".");

	private class Parser {
		private final List<String> errors;
		private final List<String> pathStack;

		public Parser() {
			errors = Lists.newArrayList();
			pathStack = Lists.newArrayList();
		}

		public Object parse(final DataTypeDescriptor descriptor, final JsonNode root)
				throws IOException {
			Object result = read(descriptor, root);
			if (errors.isEmpty()) {
				return result;
			}

			String allErrors = COMMA_JOINER.join(errors);
			throw new IOException("Parsing errors: \n" + allErrors);
		}

		private <T> T error(final String msg, final Object... args) {
			String formatted = String.format(msg, args);
			String path = DOT_JOINER.join(pathStack);
			String fullMsg = path + ": " + formatted;
			errors.add(fullMsg);
			return null;
		}

		private <T> T typeError(final String expectedType, final JsonNode node) {
			String msg = "Wrong type, must be %s, got \"%s\"";
			error(msg, expectedType, node);
			return null;
		}

		private Object read(final DataTypeDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (descriptor instanceof ValueDescriptor) {
				return readValue((ValueDescriptor) descriptor, node);
			} else if (descriptor instanceof MessageDescriptor) {
				return readMessage((MessageDescriptor) descriptor, node);
			} else if (descriptor instanceof EnumDescriptor) {
				return readEnum((EnumDescriptor) descriptor, node);
			} else if (descriptor instanceof ListDescriptor) {
				return readList((ListDescriptor) descriptor, node);
			} else if (descriptor instanceof SetDescriptor) {
				return readSet((SetDescriptor) descriptor, node);
			} else if (descriptor instanceof MapDescriptor) {
				return readMap((MapDescriptor) descriptor, node);
			} else {
				throw new IOException("Unsupported descriptor " + descriptor);
			}
		}

		private Object readValue(final ValueDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (node.isNull()) {
				return null;
			}

			Class<?> type = descriptor.getType();
			if (type == String.class) {
				return node.asText();
			} else if (type == Boolean.class) {
				return node.asBoolean();
			} else if (type == Short.class) {
				final int value = node.asInt();
				if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return (short) value;
				} else {
					error("Number overflow, short required, got " + value);
					return null;
				}
			} else if (type == Integer.class) {
				return node.asInt();
			} else if (type == Long.class) {
				return node.asLong();
			} else if (type == Float.class) {
				return (float) node.asDouble();
			} else if (type == Double.class) {
				return node.asDouble();
			}

			throw new IOException("Unsupported value descriptor " + descriptor);
		}

		private Object readMessage(final MessageDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (descriptor.isLineFormat() && node.isTextual()) {
				return lineFormat.fromJson(descriptor, node.asText());
			} else if (node.isNull()) {
				return null;
			} else if (!node.isObject()) {
				return typeError("object", node);
			}

			MessageDescriptor real = readMessageType(descriptor, node);
			Message message = real.createInstance();
			for (MessageField field : real.getFields()) {
				readField(field, message, node);
			}

			return message;
		}

		private MessageDescriptor readMessageType(final MessageDescriptor descriptor,
				final JsonNode messageNode) {
			if (!descriptor.isTypeBase()) {
				return descriptor;
			}

			MessageField typeBaseField = descriptor.getTypeBaseField();
			EnumDescriptor typeDescriptor = (EnumDescriptor) typeBaseField.getDescriptor();

			String name = typeBaseField.getName();
			JsonNode typeNode = messageNode.get(name);
			Enum<?> type = readEnum(typeDescriptor, typeNode);

			MessageDescriptor subdescriptor = descriptor.getSubtypeMap().get(type);
			if (subdescriptor == null || subdescriptor == descriptor) {
				// TODO: Log that a subtype is not found.
				return descriptor;
			}

			return readMessageType(subdescriptor, messageNode);
		}

		private void readField(final MessageField field, final Message message,
				final JsonNode messageNode) throws IOException {
			if (field.isReadOnly()) {
				return;
			}

			String name = field.getName();
			JsonNode node = messageNode.get(name);
			if (node == null || node.isMissingNode()) {
				return;
			}

			pathStack.add(name);
			DataTypeDescriptor descriptor = field.getDescriptor();
			Object value = read(descriptor, node);
			if (value != null) {
				field.set(message, value);
			}
			pathStack.remove(pathStack.size() - 1);
		}

		private Enum<?> readEnum(final EnumDescriptor descriptor, final JsonNode node) {
			if (node.isNull()) {
				return null;
			} else if (!node.isTextual()) {
				return typeError("enum string", node);
			}

			String value = node.asText().toUpperCase();
			Enum<?> enumValue = (Enum<?>) descriptor.getValues().get(value);
			if (enumValue == null) {
				return error("Unknown enum value %s", value);
			}

			return enumValue;
		}

		private Object readList(final ListDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (node.isNull()) {
				return null;
			} else if (!node.isArray()) {
				return typeError("array list", node);
			}

			DataTypeDescriptor elementDescriptor = descriptor.getElement();
			List<Object> elements = Lists.newArrayList();
			for (JsonNode elementNode : node) {
				Object element = read(elementDescriptor, elementNode);
				elements.add(element);
			}

			return elements;
		}

		private Object readSet(final SetDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (node.isNull()) {
				return null;
			} else if (!node.isArray()) {
				return typeError("array set", node);
			}

			DataTypeDescriptor elementDescriptor = descriptor.getElement();
			Set<Object> elements = Sets.newLinkedHashSet();
			for (JsonNode elementNode : node) {
				Object element = read(elementDescriptor, elementNode);
				elements.add(element);
			}

			return elements;
		}

		private Object readMap(final MapDescriptor descriptor, final JsonNode node)
				throws IOException {
			if (node.isNull()) {
				return null;
			} else if (!node.isObject()) {
				return typeError("object map", node);
			}

			DataTypeDescriptor keyDescriptor = descriptor.getKey();
			DataTypeDescriptor valueDescriptor = descriptor.getValue();

			Map<Object, Object> map = Maps.newLinkedHashMap();
			for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
				 iterator.hasNext(); ) {
				Map.Entry<String, JsonNode> e = iterator.next();
				JsonNode keyNode = new TextNode(e.getKey());
				JsonNode valueNode = e.getValue();

				Object key = read(keyDescriptor, keyNode);
				Object value = read(valueDescriptor, valueNode);

				map.put(key, value);
			}

			return map;
		}
	}
}
