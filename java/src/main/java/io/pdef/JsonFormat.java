package io.pdef;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonFormat {
	private final JsonFactory jsonFactory;

	public JsonFormat() {
		this.jsonFactory = new JsonFactory();
		jsonFactory.enable(JsonParser.Feature.ALLOW_COMMENTS);
	}

	public JsonFormat(final JsonFactory jsonFactory) {
		this.jsonFactory = jsonFactory;
	}

	public Object read(final Type type, final String s) {
		try {
			Pdef.TypeInfo info = Pdef.info(type);
			JsonParser parser = jsonFactory.createJsonParser(s);
			return read(info, parser);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	public String write(final Object object) {
		try {
			Pdef.TypeInfo info = Pdef.info(object.getClass());
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
			JsonGenerator generator = jsonFactory.createGenerator(out);
			write(info, object, generator);
			generator.close();
			return out.toString();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private Object read(final Pdef.TypeInfo info, final JsonParser parser) throws IOException {
		parser.nextToken();

		switch (info.getType()) {
			case BOOL: return parser.getValueAsBoolean();
			case INT16: return (short) parser.getValueAsInt();
			case INT32: return parser.getValueAsInt();
			case INT64: return parser.getValueAsLong();
			case FLOAT: return (float) parser.getValueAsDouble();
			case DOUBLE: return parser.getValueAsDouble();
			case STRING: return parser.getValueAsString();

			case LIST: return readList(info.asList(), parser);
			case SET: return readSet(info.asSet(), parser);
			case MAP: return readMap(info.asMap(), parser);

			case MESSAGE: return readMessage(info.asMesage(), parser);
			case ENUM: return readEnum(info.asEnum(), parser);
			case OBJECT: return readObject(info, parser);
		}
		throw new FormatException("Unsupported type " + info);
	}

	private List<?> readList(final Pdef.ListInfo info, final JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) throw new FormatException();

		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			builder.add(read(info.getElement(), parser));
		}

		return builder.build();
	}

	private Set<?> readSet(final Pdef.SetInfo info, final JsonParser parser) throws IOException {
		if (parser.getCurrentToken() != JsonToken.START_ARRAY) throw new FormatException();

		ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			builder.add(read(info.getElement(), parser));
		}

		return builder.build();
	}

	private Object readMap(final Pdef.MapInfo info, final JsonParser parser) throws IOException {
		throw new UnsupportedOperationException("StringFormat required");
	}

	private Object readMessage(final Pdef.MessageInfo info, final JsonParser parser)
			throws IOException {
		if (parser.getCurrentToken() == JsonToken.VALUE_NULL) return null;
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) throw new FormatException();

		Message.Builder builder = info.createBuilder();
		Map<String,Pdef.FieldInfo> fields = info.getFields();

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fname = parser.getCurrentName();
			Pdef.FieldInfo finfo = fields.get(fname.toLowerCase());
			if (finfo == null) continue;

			Object value = read(finfo.getType(), parser);
			finfo.set(builder, value);
		}

		return builder.build();
	}

	private Object readEnum(final Pdef.EnumInfo info, final JsonParser parser) throws IOException {
		String value = parser.getValueAsString();
		return info.getValues().get(value == null ? null : value.toUpperCase());
	}

	private Object readObject(final Pdef.TypeInfo info, final JsonParser parser) {
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

		generator.writeStartObject();
		for (Pdef.FieldInfo field : info.getFields().values()) {
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
