package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.Parser;
import io.pdef.SerializationException;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationParser;
import io.pdef.raw.RawMapInvocationParser;
import io.pdef.raw.RawParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonParser implements Parser, InvocationParser {
	private final ObjectMapper mapper;
	private final RawParser parser;
	private final RawMapInvocationParser invocationParser;

	public JsonParser(final DescriptorPool pool, final ObjectMapper mapper) {
		this.mapper = checkNotNull(mapper);
		parser = new RawParser(pool);
		invocationParser = new RawMapInvocationParser(pool);
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

		return invocationParser.parseInvocations(interfaceClass, map);
	}
}
