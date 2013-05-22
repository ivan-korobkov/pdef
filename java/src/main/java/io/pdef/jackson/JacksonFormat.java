package io.pdef.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.lang.reflect.Type;

public class JacksonFormat {
	private final ObjectMapper mapper;

	public JacksonFormat() {
		mapper = new ObjectMapper();
		mapper.registerModule(new PdefJacksonModule());

		// Configure serialization.
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.configure(SerializationFeature.INDENT_OUTPUT, true)
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		// Configure deserialization.
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
	}

	public JacksonFormat(final ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public String write(final Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public Object read(final String s, final Type type) {
		JavaType javaType = mapper.getTypeFactory().constructType(type);
		try {
			return mapper.readValue(s, javaType);
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
}
