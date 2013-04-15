package pdef.formats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.SerializationException;
import io.pdef.SerializationException;
import pdef.Message;
import pdef.TypeDescriptor;
import pdef.rpc.Call;

import java.util.List;

public class JsonSerializer implements Serializer {
	private final RawSerializer delegate;
	private final ObjectMapper mapper;

	public JsonSerializer() {
		this(new RawSerializer(), new ObjectMapper());
	}

	public JsonSerializer(final RawSerializer delegate, final ObjectMapper mapper) {
		this.delegate = delegate;
		this.mapper = mapper;
	}

	@Override
	public Object serialize(final Message message) {
		Object object = delegate.serialize(message);
		return toJson(object);
	}

	@Override
	public Object serialize(final TypeDescriptor type, final Object object) {
		Object serialized = delegate.serialize(type, object);
		return toJson(serialized);
	}

	@Override
	public Object serializeCalls(final List<Call> calls) {
		Object raw = delegate.serializeCalls(calls);
		return toJson(raw);
	}

	private Object toJson(final Object serialized) {
		try {
			return mapper.writeValueAsString(serialized);
		} catch (JsonProcessingException e) {
			throw new SerializationException(e);
		}
	}
}
