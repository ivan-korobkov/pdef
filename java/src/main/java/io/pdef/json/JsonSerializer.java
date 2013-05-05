package io.pdef.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.google.common.base.Preconditions.*;
import io.pdef.Invocation;
import io.pdef.SerializationException;
import io.pdef.Serializer;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.raw.RawSerializer;

import java.util.List;
import java.util.Map;

public class JsonSerializer implements Serializer {
	private final ObjectMapper mapper;
	private final RawSerializer serializer;

	public JsonSerializer(final DescriptorPool pool, final ObjectMapper mapper) {
		this.mapper = checkNotNull(mapper);
		serializer = new RawSerializer(pool);
	}

	@Override
	public String serialize(final Object object) {
		Object raw = serializer.serialize(object);
		return toJson(raw);
	}

	@Override
	public Object serialize(final Descriptor descriptor, final Object object) {
		Object raw = serializer.serialize(descriptor, object);
		return toJson(raw);
	}

	@Override
	public String serializeInvocations(final List<Invocation> invocations) {
		checkNotNull(invocations);
		Map<String, Map<String, Object>> raw = serializer.serializeInvocations(invocations);
		return toJson(raw);
	}

	private String toJson(final Object raw) {
		try {
			return mapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new SerializationException(e);
		}
	}
}
