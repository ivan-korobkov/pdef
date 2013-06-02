package io.pdef.formats;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonFormat {
	private final Pdef pdef;
	private final ObjectMapper mapper;
	private final StringFormat stringFormat;

	public JsonFormat() {
		this(new Pdef());
	}

	public JsonFormat(final Pdef pdef) {
		this.pdef = checkNotNull(pdef);
		mapper = new ObjectMapper();
		stringFormat = new StringFormat(pdef);
	}

	public Object read(final Type type, final String src) {
		JsonNode tree;
		try {
			tree = mapper.readTree(src);
		} catch (IOException e) {
			throw new FormatException(e);
		}

		return read(type, tree);
	}

	public Object read(final Type type, final InputStream src) {
		JsonNode tree;
		try {
			tree = mapper.readTree(src);
		} catch (IOException e) {
			throw new FormatException(e);
		}

		return read(type, tree);
	}

	public Object read(final Type type, final File src) {
		JsonNode tree;
		try {
			tree = mapper.readTree(src);
		} catch (IOException e) {
			throw new FormatException(e);
		}

		return read(type, tree);
	}

	public Object read(final Type type, final Reader src) {
		JsonNode tree;
		try {
			tree = mapper.readTree(src);
		} catch (IOException e) {
			throw new FormatException(e);
		}

		return read(type, tree);
	}

	public Object read(final Type type, final JsonNode tree) {
		try {
			PdefDescriptor info = pdef.get(type);
			return read(info, tree);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private Object read(final PdefDescriptor info, final JsonNode node) throws IOException {
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

			case MESSAGE: return readMessage(info.asMessage(), node);
			case ENUM: return readEnum(info.asEnum(), node);
			case OBJECT: return readObject(info, node);
		}
		throw new FormatException("Unsupported type " + info);
	}

	private List<?> readList(final PdefList info, final JsonNode node) throws IOException {
		if (node.isNull()) return null;

		ArrayNode arrayNode = (ArrayNode) node;
		PdefDescriptor element = info.getElement();

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode child = arrayNode.get(i);
			builder.add(read(element, child));
		}

		return builder.build();
	}

	private Set<?> readSet(final PdefSet info, final JsonNode node) throws IOException {
		if (node.isNull()) return null;

		ArrayNode arrayNode = (ArrayNode) node;
		PdefDescriptor element = info.getElement();

		ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode child = arrayNode.get(i);
			builder.add(read(element, child));
		}

		return builder.build();
	}

	private Object readMap(final PdefMap info, final JsonNode node) throws IOException {
		if (node.isNull()) return null;

		ObjectNode objectNode = (ObjectNode) node;
		PdefDescriptor key = info.getKey();
		PdefDescriptor value = info.getValue();

		ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
		Iterator<Map.Entry<String, JsonNode>> children = objectNode.fields();

		while (children.hasNext()) {
			Map.Entry<String, JsonNode> child = children.next();
			String name = child.getKey();
			JsonNode childNode = child.getValue();

			Object k = stringFormat.read(key, name);
			Object v = read(value, childNode);
			builder.put(k, v);
		}

		return builder.build();
	}

	private Object readMessage(final PdefMessage info, final JsonNode node)
			throws IOException {
		if (node.isNull()) return null;
		ObjectNode objectNode = (ObjectNode) node;

		PdefMessage polymorphic = readMessageType(info, objectNode);
		Iterator<Map.Entry<String, JsonNode>> children = objectNode.fields();
		Message.Builder builder = polymorphic.createBuilder();

		while (children.hasNext()) {
			Map.Entry<String, JsonNode> child = children.next();
			String name = child.getKey().toLowerCase();
			PdefField fd = polymorphic.getField(name);
			if (fd == null) continue;

			JsonNode childNode = child.getValue();
			Object value = read(fd.getDescriptor(), childNode);
			fd.set(builder, value);
		}

		return builder.build();
	}

	private PdefMessage readMessageType(final PdefMessage info, final JsonNode node) {
		if (!info.isPolymorphic()) return info;

		PdefField discriminator = info.getDiscriminator(); assert discriminator != null;
		JsonNode child = node.get(discriminator.getName());
		if (child == null) return info;

		String value = child.asText().toLowerCase();
		PdefMessage subinfo = info.getSubtypes().get(value);

		if (subinfo == null || subinfo == info) return info;
		return readMessageType(subinfo, node);
	}

	private Enum<?> readEnum(final PdefEnum info, final JsonNode node) {
		String value = node.asText();
		return info.getValues().get(value == null ? null : value.toUpperCase());
	}

	private Object readObject(final PdefDescriptor info, final JsonNode node) {
		return node;
	}

	// === Writing ===

	public String write(final Object object) {
		try {
			PdefDescriptor info = pdef.get(object.getClass());
			return write(object, info);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	public String write(final Object object, final PdefDescriptor info) {
		try {
			StringWriter out = new StringWriter();
			JsonGenerator generator = mapper.getFactory().createGenerator(out);
			write(object, info, generator);
			generator.close();
			return out.toString();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private void write(final Object object, final PdefDescriptor info,
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

			case MESSAGE: writeMessage(info.asMessage(), object, generator); return;
			case ENUM: writeEnum(info.asEnum(), (Enum<?>) object, generator); return;
			case OBJECT: writeObject(info, object, generator); return;
		}
		throw new FormatException("Unsupported type " + info + " " + object);
	}

	private void writeList(final PdefList info, final List<?> object,
			final JsonGenerator generator) throws IOException {
		generator.writeStartArray();
		if (object != null) {
			PdefDescriptor elementInfo = info.getElement();
			for (Object element : object) {
				write(element, elementInfo, generator);
			}
		}
		generator.writeEndArray();
	}

	private void writeSet(final PdefSet info, final Set<?> object,
			final JsonGenerator generator) throws IOException {
		generator.writeStartArray();
		if (object != null) {
			PdefDescriptor elementInfo = info.getElement();
			for (Object element : object) {
				write(element, elementInfo, generator);
			}
		}
		generator.writeEndArray();
	}

	private void writeMap(final PdefMap info, final Map<?, ?> object,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			generator.writeNull();
			return;
		}

		PdefDescriptor key = info.getKey();
		PdefDescriptor element = info.getValue();

		generator.writeStartObject();
		for (Map.Entry<?, ?> entry : object.entrySet()) {
			String name = stringFormat.write(key, entry.getKey());
			generator.writeFieldName(name);
			write(entry.getValue(), element, generator);
		}
		generator.writeEndObject();
	}

	private void writeMessage(final PdefMessage info, final Object message,
			final JsonGenerator generator) throws IOException {
		if (message == null) {
			generator.writeNull();
			return;
		}

		// Get a polymorphic message info, because a message field can point to a supertype.
		PdefMessage polymorphic = info.getSubtypes().isEmpty() ? info : (PdefMessage)
				pdef.get(message.getClass());

		generator.writeStartObject();
		for (PdefField field : polymorphic.getFields()) {
			if (!field.isSet(message)) continue;

			Object value = field.get(message);
			PdefDescriptor finfo = field.getDescriptor();
			generator.writeFieldName(field.getName());
			write(value, finfo, generator);
		}
		generator.writeEndObject();
	}

	private void writeEnum(final PdefEnum info, final Enum<?> object,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			generator.writeNull();
		} else {
			generator.writeString(object.name().toLowerCase());
		}
	}

	private void writeObject(final PdefDescriptor info, final Object object,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			generator.writeNull();
		} else if (object instanceof TreeNode) {
			generator.writeObject(object);
		} else {
			PdefDescriptor objectInfo = pdef.get(object.getClass());
			write(object, objectInfo, generator);
		}
	}
}
