package pdef.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import static com.google.common.base.Preconditions.*;
import pdef.MessageDescriptor;
import pdef.TypeDescriptor;

import java.io.IOException;
import java.util.Map;

public class JsonParser implements Parser {
	private final RawParser delegate;
	private final ObjectMapper mapper;

	public JsonParser() {
		this(new RawParser(), new ObjectMapper());
	}

	public JsonParser(final RawParser delegate, final ObjectMapper mapper) {
		this.delegate = checkNotNull(delegate);
		this.mapper = checkNotNull(mapper);
	}

	@Override
	public Object parse(final TypeDescriptor descriptor, final Object object) {
		if (object == null) return null;
		String s = (String) object;
		try {
			return parse(descriptor, s);
		} catch (IOException e) {
			throw new FormatException(e);
		}
	}

	private Object parse(final TypeDescriptor descriptor, final String s) throws IOException {
		Map map = mapper.readValue(s, Map.class);
		return delegate.parse(descriptor, map);
	}
}
