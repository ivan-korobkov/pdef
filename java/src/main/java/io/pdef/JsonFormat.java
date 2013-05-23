package io.pdef;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonFormat {
	private final ObjectMapper mapper;
	private final Pdef pdef = new Pdef();

	public JsonFormat() {
		this.mapper = new ObjectMapper();
	}

	public Object read(final Type type, final String s) {
		try {
			Pdef.TypeInfo info = pdef.get(type);
			JsonNode tree = mapper.readTree(s);
			return read(info, tree);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	public String write(final Object object) {
		try {
			Pdef.TypeInfo info = pdef.get(object.getClass());
			return write(object, info);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	public String write(final Object object, final Pdef.TypeInfo info) {
		try {
			StringWriter out = new StringWriter();
			JsonGenerator generator = mapper.getFactory().createGenerator(out);
			write(info, object, generator);
			generator.close();
			return out.toString();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private Object read(final Pdef.TypeInfo info, final JsonNode node) throws IOException {
		switch (info.getType()) {
			case BOOL: return node.asBoolean();
			case INT16: return (short) node.asInt();
			case INT32: return node.asInt();
			case INT64: return node.asLong();
			case FLOAT: return (float) node.asDouble();
			case DOUBLE: return node.asDouble();
			case STRING: return node.asText();

			case LIST: return readList(info.asList(), node);
			case SET: return readSet(info.asSet(), node);
			case MAP: return readMap(info.asMap(), node);

			case MESSAGE: return readMessage(info.asMesage(), node);
			case ENUM: return readEnum(info.asEnum(), node);
			case OBJECT: return readObject(info, node);
		}
		throw new FormatException("Unsupported type " + info);
	}

	private List<?> readList(final Pdef.ListInfo info, final JsonNode node) throws IOException {
		if (node.isNull()) return null;

		ArrayNode arrayNode = (ArrayNode) node;
		Pdef.TypeInfo element = info.getElement();

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode child = arrayNode.get(i);
			builder.add(read(element, child));
		}

		return builder.build();
	}

	private Set<?> readSet(final Pdef.SetInfo info, final JsonNode node) throws IOException {
		if (node.isNull()) return null;

		ArrayNode arrayNode = (ArrayNode) node;
		Pdef.TypeInfo element = info.getElement();

		ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode child = arrayNode.get(i);
			builder.add(read(element, child));
		}

		return builder.build();
	}

	private Object readMap(final Pdef.MapInfo info, final JsonNode node) throws IOException {
		throw new UnsupportedOperationException("StringFormat required");
	}

	private Object readMessage(final Pdef.MessageInfo info, final JsonNode node)
			throws IOException {
		if (node.isNull()) return null;
		ObjectNode objectNode = (ObjectNode) node;

		Pdef.MessageInfo polymorphic = readMessageType(info, objectNode);
		Map<String, Pdef.FieldInfo> fields = polymorphic.getFields();
		Iterator<Map.Entry<String, JsonNode>> children = objectNode.fields();
		Message.Builder builder = polymorphic.createBuilder();

		while (children.hasNext()) {
			Map.Entry<String, JsonNode> child = children.next();
			String name = child.getKey().toLowerCase();
			if (!fields.containsKey(name)) continue;

			JsonNode childNode = child.getValue();
			Pdef.FieldInfo finfo = fields.get(name);
			Object value = read(finfo.getType(), childNode);
			finfo.set(builder, value);
		}

		return builder.build();
	}

	private Pdef.MessageInfo readMessageType(final Pdef.MessageInfo info, final JsonNode node) {
		if (!info.isPolymorphic()) return info;

		Pdef.FieldInfo discriminator = info.getDiscriminator();
		JsonNode child = node.get(discriminator.getName());
		if (child == null) return info;

		String value = child.asText().toLowerCase();
		Pdef.MessageInfo subinfo = info.getSubtypes().get(value);

		if (subinfo == null || subinfo == info) return info;
		return readMessageType(subinfo, node);
	}

	private Enum<?> readEnum(final Pdef.EnumInfo info, final JsonNode node) {
		String value = node.asText();
		return info.getValues().get(value == null ? null : value.toUpperCase());
	}

	private Object readObject(final Pdef.TypeInfo info, final JsonNode node) {
		throw new FormatException("Unsupported operation");
	}

	// === Writing ===

	private void write(final Pdef.TypeInfo info, final Object object,
			final JsonGenerator generator) throws IOException {
		switch (info.getType()) {
			case BOOL: generator.writeBoolean(object == null ? false : (Boolean) object); return;
			case INT16: generator.writeNumber(object == null ? 0 : (Short) object); return;
			case INT32: generator.writeNumber(object == null ? 0 : (Integer) object); return;
			case INT64: generator.writeNumber(object == null ? 0 : (Long) object); return;
			case FLOAT: generator.writeNumber(object == null ? 0 : (Float) object); return;
			case DOUBLE: generator.writeNumber(object == null ? 0 : (Double) object); return;
			case STRING: generator.writeString((String) object); return;

			case LIST: writeList(info.asList(), (List) object, generator); return;
			case SET: writeSet(info.asSet(), (Set) object, generator); return;
			case MAP: writeMap(info.asMap(), (Map) object, generator); return;

			case MESSAGE: writeMessage(info.asMesage(), object, generator); return;
			case ENUM: writeEnum(info.asEnum(), (Enum<?>) object, generator); return;
			case OBJECT: writeObject(info, object, generator); return;
		}
		throw new FormatException("Unsupported type " + info + " " + object);
	}

	private void writeList(final Pdef.ListInfo info, final List<?> object,
			final JsonGenerator generator) throws IOException {
		generator.writeStartArray();
		if (object != null) {
			Pdef.TypeInfo elementInfo = info.getElement();
			for (Object element : object) {
				write(elementInfo, element, generator);
			}
		}
		generator.writeEndArray();
	}

	private void writeSet(final Pdef.SetInfo info, final Set<?> object,
			final JsonGenerator generator) throws IOException {
		generator.writeStartArray();
		if (object != null) {
			Pdef.TypeInfo elementInfo = info.getElement();
			for (Object element : object) {
				write(elementInfo, element, generator);
			}
		}
		generator.writeEndArray();
	}

	private void writeMap(final Pdef.MapInfo info, final Map<?, ?> object,
			final JsonGenerator generator) {
		throw new FormatException("Unsupported operation, StringFormat required");
	}

	private void writeMessage(final Pdef.MessageInfo info, final Object message,
			final JsonGenerator generator) throws IOException {
		if (message == null) {
			generator.writeNull();
			return;
		}

		// Get a polymorphic message info, because a message field can point to a supertype.
		Pdef.MessageInfo polymorphic = info.getSubtypes().isEmpty() ? info : (Pdef.MessageInfo)
				pdef.get(message.getClass());

		generator.writeStartObject();
		for (Pdef.FieldInfo field : polymorphic.getFields().values()) {
			if (!field.isSet(message)) continue;

			Object value = field.get(message);
			Pdef.TypeInfo finfo = field.getType();
			generator.writeFieldName(field.getName());
			write(finfo, value, generator);
		}
		generator.writeEndObject();
	}

	private void writeEnum(final Pdef.EnumInfo info, final Enum<?> object,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			generator.writeNull();
		} else {
			generator.writeString(object.name().toLowerCase());
		}
	}

	private void writeObject(final Pdef.TypeInfo info, final Object object,
			final JsonGenerator generator) {
		throw new FormatException("Unsupported operation");
	}
}
