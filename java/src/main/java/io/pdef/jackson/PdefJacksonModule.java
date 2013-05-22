package io.pdef.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;

public class PdefJacksonModule extends Module {

	@Override
	public String getModuleName() {
		return "pdef-jackson";
	}

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

	@Override
	public void setupModule(final SetupContext context) {
		context.appendAnnotationIntrospector(new PdefJacksonAnnotationIntrospector());
		context.addSerializers(new Serializers.Base() {
			@SuppressWarnings("unchecked")
			@Override
			public JsonSerializer<?> findSerializer(final SerializationConfig config,
					final JavaType type, final BeanDescription beanDesc) {
				Class<?> cls = type.getRawClass();
				if (!Enum.class.isAssignableFrom(cls)) return null;
				return new LowercaseEnumSerializer((Class<Enum<?>>) cls);
			}
		});
		context.addDeserializers(new Deserializers.Base() {
			@SuppressWarnings("unchecked")
			@Override
			public JsonDeserializer<?> findEnumDeserializer(final Class<?> type,
					final DeserializationConfig config, final BeanDescription beanDesc)
					throws JsonMappingException {
				if (!Enum.class.isAssignableFrom(type)) return null;
				return new CaseInsensitiveEnumDeserializer((Class<Enum<?>>) type);
			}
		});
	}
}
