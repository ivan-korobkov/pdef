package io.pdef.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.SerializationException;
import io.pdef.Serializer;
import io.pdef.raw.RawSerializer;
import io.pdef.SerializationException;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonSerializer implements Serializer {
	private final RawSerializer serializer;
	private final ObjectMapper mapper;

	public JsonSerializer(final RawSerializer serializer, final ObjectMapper mapper) {
		this.serializer = checkNotNull(serializer);
		this.mapper = checkNotNull(mapper);
	}

	@Override
	public String serialize(final Object object) {
		Object raw = serializer.serialize(object);
		try {
			return mapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new SerializationException(e);
		}
	}
}
