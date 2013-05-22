package io.pdef.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsArrayTypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkState;

public class PdefTypeResolver extends StdTypeResolverBuilder {
	@Override
	public TypeSerializer buildTypeSerializer(final SerializationConfig config,
			final JavaType baseType, final Collection<NamedType> subtypes) {
		if (_idType == JsonTypeInfo.Id.NONE) return null;

		TypeIdResolver idResolver = idResolver(config, baseType, subtypes, true, false);
		checkState(_includeAs == JsonTypeInfo.As.PROPERTY,
				"Pdef type serializer is only supported to PROPERTY inclusion, not for "
						+ _includeAs);

		return new PdefTypeSerializer(idResolver, null);
	}

	@Override
	public TypeDeserializer buildTypeDeserializer(final DeserializationConfig config,
			final JavaType baseType, final Collection<NamedType> subtypes) {
		return super.buildTypeDeserializer(config, baseType, subtypes);
	}

	static class PdefTypeSerializer extends AsArrayTypeSerializer {
		public PdefTypeSerializer(final TypeIdResolver idRes, final BeanProperty property) {
			super(idRes, property);
		}

		@Override
		public JsonTypeInfo.As getTypeInclusion() {
			return JsonTypeInfo.As.PROPERTY;
		}

		@Override
		public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException {
			jgen.writeStartObject();
		}

		@Override
		public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type)
				throws IOException {
			jgen.writeStartObject();
		}

		@Override
		public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
				throws IOException, JsonProcessingException
		{
			jgen.writeEndObject();
		}

		@Override
		public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId)
				throws IOException, JsonProcessingException {
			jgen.writeStartObject();
		}

		@Override
		public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId)
				throws IOException, JsonProcessingException {
			jgen.writeEndObject();
		}
	}
}
