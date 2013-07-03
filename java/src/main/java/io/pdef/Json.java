package io.pdef;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class Json {
	private static final JsonFactory FACTORY = new JsonFactory()
			.enable(JsonParser.Feature.ALLOW_COMMENTS)
			.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	private Json() {}

	/** Parses a string and returns a primitive or a collection.
	 * @throws JsonException if wrong json or any another exception. */
	public static Object parse(final String s) throws JsonException {
		checkNotNull(s);
		try {
			JsonParser parser = FACTORY.createParser(s);
			try {
				parser.nextToken();
				return parseObject(parser);
			} finally {
				parser.close();
			}
		} catch (Exception e) {
			if (e instanceof JsonException) throw (JsonException) e;
			throw new JsonException(e);
		}
	}

	private static Object parseObject(final JsonParser parser) throws IOException {
		JsonToken token = parser.getCurrentToken();
		if (token == null) return null;

		switch (token) {
			case VALUE_NULL: return null;
			case VALUE_TRUE: return true;
			case VALUE_FALSE: return false;
			case VALUE_STRING: return parser.getValueAsString();
			case VALUE_NUMBER_INT: return parser.getLongValue();
			case VALUE_NUMBER_FLOAT: return parser.getDoubleValue();
			case START_ARRAY: return parseArray(parser);
			case START_OBJECT: return parseMap(parser);
			default: throw new JsonException("Bad json");
		}
	}

	private static List<?> parseArray(final JsonParser parser) throws IOException {
		JsonToken token = parser.getCurrentToken();
		if (token != JsonToken.START_ARRAY) throw new IOException("Expected an array start");

		List<Object> list = Lists.newArrayList();
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			Object element = parseObject(parser);
			list.add(element);
		}

		return list;
	}

	private static Map<String, Object> parseMap(final JsonParser parser) throws IOException {
		JsonToken token = parser.getCurrentToken();
		if (token != JsonToken.START_OBJECT) throw new IOException("Expected an object start");

		Map<String, Object> map = Maps.newLinkedHashMap();
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String field = parser.getCurrentName();
			parser.nextToken();
			Object value = parseObject(parser);
			map.put(field, value);
		}

		return map;
	}

	/** Serializes a primitive or a collection into a JSON string w/o indentation.
	 * @throws JsonException if any exception. */
	public static String serialize(final Object o) throws JsonException {
		return serialize(o, false);
	}

	/** Serializes a primitive or a collection into a JSON string with an optional indentation.
	 * @throws JsonException if any exception. */
	public static String serialize(final Object o, boolean indent) {
		try {
			StringWriter out = new StringWriter();
			JsonGenerator generator = FACTORY.createGenerator(out);
			if (indent) generator.useDefaultPrettyPrinter();

			writeObject(generator, o);
			generator.flush();

			return out.toString();
		} catch (Exception e) {
			if (e instanceof JsonException) throw (JsonException) e;
			throw new JsonException(e);
		}
	}

	private static void writeObject(final JsonGenerator generator, final Object o)
			throws IOException {
		if (o == null) generator.writeNull();
		else if (o instanceof List) writeList(generator, (Collection<?>) o);
		else if (o instanceof Set) writeList(generator, (Collection<?>) o);
		else if (o instanceof Map) writeMap(generator, (Map<?, ?>) o);
		else generator.writeObject(o); // It's smart enough to correctly write all other values.
	}

	private static void writeList(final JsonGenerator generator, final Collection<?> list)
			throws IOException {
		if (list == null) {
			generator.writeNull();
			return;
		}

		generator.writeStartArray();
		for (Object o : list) writeObject(generator, o);
		generator.writeEndArray();
	}

	private static void writeMap(final JsonGenerator generator, final Map<?, ?> map)
			throws IOException {
		if (map == null) {
			generator.writeNull();
			return;
		}

		generator.writeStartObject();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();
			generator.writeFieldName(key);
			writeObject(generator, value);
		}
		generator.writeEndObject();
	}
}
