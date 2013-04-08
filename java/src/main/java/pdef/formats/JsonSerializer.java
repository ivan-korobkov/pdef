package pdef.formats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pdef.Message;
import pdef.TypeDescriptor;

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
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new FormatException(e);
		}
	}

	@Override
	public Object serialize(final TypeDescriptor type, final Object object) {
		Object serialized = delegate.serialize(type, object);
		try {
			return mapper.writeValueAsString(serialized);
		} catch (JsonProcessingException e) {
			throw new FormatException(e);
		}
	}
}
