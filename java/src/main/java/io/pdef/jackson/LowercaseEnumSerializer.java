package io.pdef.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

public class LowercaseEnumSerializer extends StdScalarSerializer<Enum<?>> {
	public LowercaseEnumSerializer(final Class<Enum<?>> type) {
		super(type);
	}

	@Override
	public void serialize(final Enum<?> value, final JsonGenerator jgen,
			final SerializerProvider provider) throws IOException {
		if (value == null) {
			jgen.writeNull();
		} else {
			String name = value.name().toLowerCase();
			jgen.writeString(name);
		}
	}
}
