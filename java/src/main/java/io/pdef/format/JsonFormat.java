package io.pdef.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.pdef.Message;
import io.pdef.descriptors.*;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JsonFormat parses and serializes Pdef data types from/to JSON. */
public class JsonFormat implements Format {
	private static final JsonFactory FACTORY = new JsonFactory()
			.enable(JsonParser.Feature.ALLOW_COMMENTS)
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
	private static final JsonFormat INSTANCE = new JsonFormat();

	public static JsonFormat instance() {
		return INSTANCE;
	}

	private final NativeFormat nativeFormat;

	private JsonFormat() {
		nativeFormat = NativeFormat.instance();
	}

	// Serialization.

	/** Serializes an object into a string. */
	public <T> String serialize(final T object, final DataDescriptor<T> descriptor,
			final boolean indent) throws FormatException {
		checkNotNull(descriptor);

		try {
			StringWriter out = new StringWriter();
			JsonGenerator generator = FACTORY.createGenerator(out);
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
	public <T> void serialize(final OutputStream out, final T object,
			final DataDescriptor<T> descriptor, final boolean indent) {
		checkNotNull(out);
		checkNotNull(descriptor);

		try {
			JsonGenerator generator = FACTORY.createGenerator(out);
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

	/** Writes an object to a writer as a JSON string, does not close the write. */
	public <T> void serialize(final PrintWriter writer, final T object,
			final DataDescriptor<T> descriptor, final boolean indent) {
		checkNotNull(writer);
		checkNotNull(descriptor);

		try {
			JsonGenerator generator = FACTORY.createGenerator(writer);
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

	private <T> void write(final T object, final DataDescriptor<T> descriptor,
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
			case EXCEPTION:
				writeMessage((Message) object, generator);
				return;
			default:
				throw new FormatException("Unsupported descriptor " + descriptor);
		}
	}

	private <T> void writeList(@Nonnull final List<T> object, final ListDescriptor<T> descriptor,
			final JsonGenerator generator) throws IOException {
		DataDescriptor<T> element = descriptor.getElement();

		generator.writeStartArray();
		for (T elem : object) {
			write(elem, element, generator);
		}
		generator.writeEndArray();
	}

	private <T> void writeSet(@Nonnull final Set<T> object, final SetDescriptor<T> descriptor,
			final JsonGenerator generator) throws IOException {
		DataDescriptor<T> element = descriptor.getElement();

		generator.writeStartArray();
		for (T elem : object) {
			write(elem, element, generator);
		}
		generator.writeEndArray();
	}

	private <K, V> void writeMap(@Nonnull final Map<K, V> object,
			final MapDescriptor<K, V> descriptor, final JsonGenerator generator)
			throws IOException {
		DataDescriptor<K> key = descriptor.getKey();
		DataDescriptor<V> value = descriptor.getValue();

		generator.writeStartObject();
		for (Map.Entry<K, V> entry : object.entrySet()) {
			K k = entry.getKey();
			V v = entry.getValue();

			writeFieldName(k, key, generator);
			write(v, value, generator);
		}
		generator.writeEndObject();
	}

	private <K> void writeFieldName(final K object, final DataDescriptor<K> descriptor,
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

		DataDescriptor<V> type = field.getType();
		generator.writeFieldName(field.getName());
		write(value, type, generator);
	}

	// Parsing.

	/** Parses an object from a string. */
	public <T> T parse(final String input, final DataDescriptor<T> descriptor)
			throws FormatException {
		checkNotNull(descriptor);
		if (input == null) {
			return null;
		}

		try {
			JsonParser parser = FACTORY.createParser(input);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Parses an object from an input stream, does not close the input stream. */
	public <T> T parse(final InputStream input, final DataDescriptor<T> descriptor) {
		checkNotNull(input);
		checkNotNull(descriptor);

		try {
			JsonParser parser = FACTORY.createParser(input);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	/** Parses an object from a reader, does not close the reader. */
	public <T> T parse(final Reader reader, final DataDescriptor<T> descriptor) {
		checkNotNull(reader);
		checkNotNull(descriptor);

		try {
			JsonParser parser = FACTORY.createParser(reader);
			return read(parser, descriptor);
		} catch (FormatException e) {
			throw e;
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}

	private <T> T read(final JsonParser parser, final DataDescriptor<T> descriptor)
			throws IOException {
		Object nativeObject;

		try {
			parser.nextToken();
			nativeObject = read(parser);
		} finally {
			parser.close();
		}

		return nativeFormat.parse(nativeObject, descriptor);
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
				return parser.getIntValue();
			case VALUE_NUMBER_FLOAT:
				return parser.getDoubleValue();
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
			throw new FormatException("Bad JSON string, failed to parse an array");
		}

		List<Object> list = Lists.newArrayList();
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
			throw new FormatException("Bad JSON string, failed to parse an object");
		}

		Map<String, Object> map = Maps.newLinkedHashMap();
		while (true) {
			JsonToken next = parser.nextToken();
			if (next == null) {
				throw new FormatException("End of file");
			} else if (next == JsonToken.END_OBJECT) {
				break;
			} else if (next != JsonToken.FIELD_NAME) {
				throw new FormatException("Failed to parse a field name from " + next);
			}

			String field = parser.getCurrentName();
			parser.nextToken();
			Object value = read(parser);
			map.put(field, value);
		}

		return map;
	}
}
