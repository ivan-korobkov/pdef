package com.ivankorobkov.pdef.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.google.inject.Inject;
import com.ivankorobkov.pdef.data.Message;
import com.ivankorobkov.pdef.data.MessageDescriptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class PdefJacksonModule extends Module {

	private final JsonFormat jsonFormat;
	private final String name;
	private final Version version;

	@Inject
	public PdefJacksonModule(final JsonFormat jsonFormat) {
		this.jsonFormat = jsonFormat;

		name = "pdef-jackson-module";
		version = Version.unknownVersion();
	}

	@Override
	public String getModuleName() {
		return name;
	}

	@Override
	public Version version() {
		return version;
	}

	@Override
	public void setupModule(final SetupContext context) {
		context.addSerializers(new SimpleSerializers(){
			@Override
			public JsonSerializer<?> findSerializer(final SerializationConfig config,
					final JavaType type, final BeanDescription beanDesc) {
				Class<?> cls = type.getRawClass();
				if (!Message.class.isAssignableFrom(cls)) {
					return null;
				}

				return new Serializer(jsonFormat);
			}
		});

		context.addDeserializers(new SimpleDeserializers(){
			@Override
			public JsonDeserializer<?> findBeanDeserializer(final JavaType type,
					final DeserializationConfig config,
					final BeanDescription beanDesc) throws JsonMappingException {
				Class<?> cls = type.getRawClass();
				if (!Message.class.isAssignableFrom(cls)) {
					return null;
				}

				@SuppressWarnings("unchecked")
				Class<Message> messageClass = (Class<Message>) cls;
				MessageDescriptor descriptor = getDescriptor(messageClass);
				return new Deserializer(descriptor, jsonFormat);
			}
		});
	}

	private MessageDescriptor getDescriptor(Class<Message> messageClass) {
		Message message;
		try {
			message = messageClass.getConstructor().newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		return message.getDescriptor();
	}


	public static class Serializer extends JsonSerializer<Message> {

		private final JsonFormat jsonFormat;

		public Serializer(final JsonFormat jsonFormat) {
			this.jsonFormat = jsonFormat;
		}

		@Override
		public void serialize(final Message value, final JsonGenerator jgen,
				final SerializerProvider provider) throws IOException, JsonProcessingException {
			jsonFormat.serialize(value, jgen);
		}
	}

	public static class Deserializer extends JsonDeserializer<Message> {

		private final MessageDescriptor descriptor;
		private final JsonFormat jsonFormat;

		public Deserializer(final MessageDescriptor descriptor, final JsonFormat jsonFormat) {
			this.descriptor = descriptor;
			this.jsonFormat = jsonFormat;
		}

		@Override
		public Message deserialize(final JsonParser jp, final DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return jsonFormat.deserialize(descriptor, jp);
		}
	}
}
