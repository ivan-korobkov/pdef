package io.pdef.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.SerializationException;
import io.pdef.Serializer;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.invocation.Invocation;
import io.pdef.invocation.InvocationSerializer;
import io.pdef.raw.RawMapInvocationSerializer;
import io.pdef.raw.RawSerializer;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonSerializer implements Serializer, InvocationSerializer {
	private final ObjectMapper mapper;
	private final RawSerializer serializer;
	private final RawMapInvocationSerializer invocationSerializer;

	public JsonSerializer(final DescriptorPool pool, final ObjectMapper mapper) {
		this.mapper = checkNotNull(mapper);
		serializer = new RawSerializer(pool);
		invocationSerializer = new RawMapInvocationSerializer(pool);
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

	@Override
	public String serializeInvocations(final List<Invocation> invocations) {
		checkNotNull(invocations);
		Map<String, List<Object>> raw = invocationSerializer.serializeInvocations(invocations);
		try {
			return mapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new SerializationException(e);
		}
	}
}
