package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.SerializationException;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationParser;
import io.pdef.raw.RawMapInvocationParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonMapInvocationParser implements InvocationParser {
	private final ObjectMapper mapper;
	private final RawMapInvocationParser parser;

	public JsonMapInvocationParser(final ObjectMapper mapper, final DescriptorPool pool) {
		this.mapper = checkNotNull(mapper);
		this.parser = new RawMapInvocationParser(pool);
	}

	@Override
	public List<Invocation> parse(final Class<?> interfaceClass, final Object object) {
		Map map;
		try {
			map = mapper.readValue((String) object, Map.class);
		} catch (IOException e) {
			throw new SerializationException(e);
		}

		return parser.parse(interfaceClass, map);
	}
}
