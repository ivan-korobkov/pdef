package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.Parser;
import io.pdef.SerializationException;
import io.pdef.raw.RawParser;
import io.pdef.SerializationException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonParser implements Parser {
	private final ObjectMapper mapper;
	private final RawParser parser;

	public JsonParser(final RawParser parser, final ObjectMapper mapper) {
		this.mapper = checkNotNull(mapper);
		this.parser = checkNotNull(parser);
	}

	@Override
	public Object parse(final Type type, final Object object) {
		checkNotNull(type);

		if (object == null) return null;
		String s = (String) object;

		Map<?, ?> map;
		try {
			map = mapper.readValue(s, Map.class);
		} catch (IOException e) {
			throw new SerializationException(e);
		}

		return parser.parse(type, map);
	}
}
