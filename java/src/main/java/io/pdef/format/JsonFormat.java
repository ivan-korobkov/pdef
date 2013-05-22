package io.pdef.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.pdef.Pdef;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
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

	public Object read(final Type type, final String s) throws IOException {
		Pdef.TypeInfo info = Pdef.info(type);
		JsonParser parser = jsonFactory.createJsonParser(s);

		return read(info, parser);
	}

	private Object read(final Pdef.TypeInfo info, final JsonParser parser) throws IOException {
		switch (info.getType()) {
			case BOOL: return parser.getValueAsBoolean();
			case INT16: return (short) parser.getValueAsInt();
			case INT32: return parser.getValueAsInt();
			case INT64: return parser.getValueAsLong();
			case FLOAT: return (float) parser.getValueAsDouble();
			case DOUBLE: return parser.getValueAsDouble();
			case STRING: return parser.getValueAsString();
			case OBJECT: return readObject(info, parser);
			case LIST: return readList(info.asList(), parser);
			case SET: return readSet(info.asSet(), parser);
			case MAP: return readMap(info.asMap(), parser);
			case MESSAGE: return readMessage(info.asMesage(), parser);
			case ENUM: return readEnum(info.asEnum(), parser);
		}
		throw new IllegalArgumentException("Unsupported type " + info);
	}

	private Object readObject(final Pdef.TypeInfo info, final JsonParser parser) {
		return null;
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

		TreeNode tree = parser.readValueAsTree();
		tree.traverse();
		return null;
	}

	private Object readEnum(final Pdef.EnumInfo enumInfo, final JsonParser parser) {
		return null;
	}
}
