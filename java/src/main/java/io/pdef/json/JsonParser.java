package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.Parser;
import io.pdef.SerializationException;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.Invocation;
import io.pdef.raw.RawParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonParser implements Parser {
	private final ObjectMapper mapper;
	private final RawParser parser;

	public JsonParser(final DescriptorPool pool, final ObjectMapper mapper) {
		this.mapper = checkNotNull(mapper);
		parser = new RawParser(pool);
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

	@Override
	public List<Invocation> parseInvocations(final Class<?> interfaceClass, final Object object) {
		Map map;
		try {
			map = mapper.readValue((String) object, Map.class);
		} catch (IOException e) {
			throw new SerializationException(e);
		}

		return parser.parseInvocations(interfaceClass, map);
	}
}
