package io.pdef.formats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.pdef.*;
import io.pdef.descriptors.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

/** JsonFormat parses and serializes Pdef value types from/to JSON. */
public class JsonFormat {
	private static final JsonFormat INSTANCE = new JsonFormat();

	private final JsonFactory factory;
	private final ObjectFormat objectFormat;

	protected JsonFormat() {
		this(new JsonFactory().enable(JsonParser.Feature.ALLOW_COMMENTS));
	}

	protected JsonFormat(final JsonFactory factory) {
		this.factory = factory;
		objectFormat = ObjectFormat.getInstance();
	}

	public static JsonFormat getInstance() {
		return INSTANCE;
	}

	// Serialization.

	/** Serializes an object into a string. */
	public <T> String toJson(final T object, final DataTypeDescriptor<T> descriptor,
			final boolean indent) throws FormatException {
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			StringWriter out = new StringWriter();
			JsonGenerator generator = factory.createGenerator(out);
			if (indent) {
				generator.useDefaultPrettyPrinter();
			}

			write(object, descriptor, generator);
			generator.flush();

			return out.toString();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Writes an object to an output stream as a JSON string, does not close the stream. */
	public <T> void toJson(final OutputStream out, final T object,
			final DataTypeDescriptor<T> descriptor, final boolean indent) {
		if (out == null) throw new NullPointerException("out");
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			JsonGenerator generator = factory.createGenerator(out);
			if (indent) {
				generator.useDefaultPrettyPrinter();
			}

			write(object, descriptor, generator);
			generator.flush();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Writes an object to a writer as a JSON string, does not close the writer. */
	public <T> void toJson(final PrintWriter writer, final T object,
			final DataTypeDescriptor<T> descriptor, final boolean indent) {
		if (writer == null) throw new NullPointerException("writer");
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			JsonGenerator generator = factory.createGenerator(writer);
			if (indent) {
				generator.useDefaultPrettyPrinter();
			}

			write(object, descriptor, generator);
			generator.flush();
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private <T> void write(final T object, final DataTypeDescriptor<T> descriptor,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			generator.writeNull();
			return;
		}

		switch (descriptor.getType()) {
			case BOOL:
				generator.writeBoolean((Boolean) object);
				return;
			case INT16:
				generator.writeNumber((Short) object);
				return;
			case INT32:
				generator.writeNumber((Integer) object);
				return;
			case INT64:
				generator.writeNumber((Long) object);
				return;
			case FLOAT:
				generator.writeNumber((Float) object);
				return;
			case DOUBLE:
				generator.writeNumber((Double) object);
				return;
			case STRING:
				generator.writeString((String) object);
				return;
			case LIST:
				writeList((List) object, (ListDescriptor) descriptor, generator);
				return;
			case SET:
				writeSet((Set) object, (SetDescriptor) descriptor, generator);
				return;
			case MAP:
				writeMap((Map) object, (MapDescriptor) descriptor, generator);
				return;
			case VOID:
				generator.writeNull();
				return;
			case ENUM:
				writeEnum((Enum) object, generator);
				return;
			case MESSAGE:
				writeMessage((Message) object, generator);
				return;
			default:
				throw new FormatException("Unsupported descriptor " + descriptor);
		}
	}

	private <T> void writeList(@Nonnull final List<T> object, final ListDescriptor<T> descriptor,
			final JsonGenerator generator) throws IOException {
		DataTypeDescriptor<T> element = descriptor.getElement();

		generator.writeStartArray();
		for (T elem : object) {
			write(elem, element, generator);
		}
		generator.writeEndArray();
	}

	private <T> void writeSet(@Nonnull final Set<T> object, final SetDescriptor<T> descriptor,
			final JsonGenerator generator) throws IOException {
		DataTypeDescriptor<T> element = descriptor.getElement();

		generator.writeStartArray();
		for (T elem : object) {
			write(elem, element, generator);
		}
		generator.writeEndArray();
	}

	private <K, V> void writeMap(@Nonnull final Map<K, V> object,
			final MapDescriptor<K, V> descriptor, final JsonGenerator generator)
			throws IOException {
		DataTypeDescriptor<K> key = descriptor.getKey();
		DataTypeDescriptor<V> value = descriptor.getValue();

		generator.writeStartObject();
		for (Map.Entry<K, V> entry : object.entrySet()) {
			K k = entry.getKey();
			V v = entry.getValue();

			writeFieldName(k, key, generator);
			write(v, value, generator);
		}
		generator.writeEndObject();
	}

	private <K> void writeFieldName(final K object, final DataTypeDescriptor<K> descriptor,
			final JsonGenerator generator) throws IOException {
		if (object == null) {
			throw new FormatException(
					"Null key, the key must be a non-null primitive or enum, " + descriptor);
		}

		switch (descriptor.getType()) {
			case BOOL:
			case INT16:
			case INT32:
			case INT64:
			case FLOAT:
			case DOUBLE:
			case STRING:
				generator.writeFieldName(object.toString());
				return;
			case ENUM:
				generator.writeFieldName(((Enum) object).name().toLowerCase());
				return;
			default:
				throw new FormatException("Unsupported map key descriptor " + descriptor);
		}
	}

	private <T extends Enum<T>> void writeEnum(@Nonnull final T object,
			final JsonGenerator generator) throws IOException {
		String value = object.name().toLowerCase();
		generator.writeString(value);
	}

	private <T extends Message> void writeMessage(@Nonnull final T object,
			final JsonGenerator generator) throws IOException {
		// Mind polymorphic messages.
		@SuppressWarnings("unchecked")
		MessageDescriptor<T> polymorphic = (MessageDescriptor<T>) object.descriptor();

		generator.writeStartObject();
		for (FieldDescriptor<? super T, ?> field : polymorphic.getFields()) {
			writeMessageField(field, object, generator);
		}
		generator.writeEndObject();
	}

	private <T extends Message, V> void writeMessageField(
			final FieldDescriptor<? super T, V> field, final T message,
			final JsonGenerator generator) throws IOException {
		V value = field.get(message);
		if (value == null) {
			// Skip null fields.
			return;
		}

		DataTypeDescriptor<V> type = field.getType();
		generator.writeFieldName(field.getName());
		write(value, type, generator);
	}

	// Parsing.

	/** Parses an object from a string. */
	public <T> T fromJson(final String input, final DataTypeDescriptor<T> descriptor)
			throws FormatException {
		if (descriptor == null) throw new NullPointerException("descriptor");
		if (input == null) {
			return null;
		}

		try {
			JsonParser parser = factory.createParser(input);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Parses an object from an input stream, does not close the input stream. */
	public <T> T fromJson(final InputStream input, final DataTypeDescriptor<T> descriptor) {
		if (input == null) throw new NullPointerException("input");
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			JsonParser parser = factory.createParser(input);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Parses an object from a reader, does not close the reader. */
	public <T> T fromJson(final Reader reader, final DataTypeDescriptor<T> descriptor) {
		if (reader == null) throw new NullPointerException("reader");
		if (descriptor == null) throw new NullPointerException("descriptor");

		try {
			JsonParser parser = factory.createParser(reader);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private <T> T read(final JsonParser parser, final DataTypeDescriptor<T> descriptor)
			throws IOException {
		Object nativeObject;

		try {
			parser.nextToken();
			nativeObject = read(parser);
		} finally {
			parser.close();
		}

		return objectFormat.fromObject(nativeObject, descriptor);
	}

	private Object read(final JsonParser parser) throws IOException {
		JsonToken current = parser.getCurrentToken();
		if (current == null || current == JsonToken.VALUE_NULL) {
			return null;
		}

		switch (current) {
			case VALUE_NULL:
				return null;
			case VALUE_TRUE:
				return true;
			case VALUE_FALSE:
				return false;
			case VALUE_STRING:
				return parser.getValueAsString();
			case VALUE_NUMBER_INT:
			case VALUE_NUMBER_FLOAT:
				return parser.getNumberValue();
			case START_ARRAY:
				return readArray(parser);
			case START_OBJECT:
				return readMap(parser);
			default:
				throw new FormatException("Bad JSON string");
		}
	}

	private List<?> readArray(final JsonParser parser) throws IOException {
		JsonToken current = parser.getCurrentToken();
		if (current != JsonToken.START_ARRAY) {
			throw new FormatException("Bad JSON string, failed to fromJson an array");
		}

		List<Object> list = new ArrayList<Object>();
		while (true) {
			JsonToken next = parser.nextToken();
			if (next == null) {
				throw new FormatException("End of file");
			} else if (next == JsonToken.END_ARRAY) {
				break;
			}

			Object element = read(parser);
			list.add(element);
		}

		return list;
	}

	private Map<String, Object> readMap(final JsonParser parser) throws IOException {
		JsonToken current = parser.getCurrentToken();
		if (current != JsonToken.START_OBJECT) {
			throw new FormatException("Bad JSON string, failed to fromJson an object");
		}

		Map<String, Object> map = new LinkedHashMap<String, Object>();
		while (true) {
			JsonToken next = parser.nextToken();
			if (next == null) {
				throw new FormatException("End of file");
			} else if (next == JsonToken.END_OBJECT) {
				break;
			} else if (next != JsonToken.FIELD_NAME) {
				throw new FormatException("Failed to fromJson a field name from " + next);
			}

			String field = parser.getCurrentName();
			parser.nextToken();
			Object value = read(parser);
			map.put(field, value);
		}

		return map;
	}
}
