/*
 * Copyright: 2013 Pdef <http://pdef.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/** JSON serialization for pdef types. */
public class PdefJson {
	private static final ObjectMapper mapper;

	static {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		mapper = new ObjectMapper();
		mapper.setDateFormat(dateFormat);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private PdefJson() {}

	public static ObjectMapper mapper() {
		return mapper;
	}

	public static Module module() {
		return new InternalModule();
	}

	public static Object parse(final String s, final Type type) {
		try {
			JavaType javaType = mapper.constructType(type);
			return mapper.readValue(s, javaType);
		} catch (IOException e) {
			throw new PdefException("JSON deserialization error", e);
		}
	}

	public static <T> T parse(final String s, final Class<T> cls) {
		try {
			return mapper.readValue(s, cls);
		} catch (IOException e) {
			throw new PdefException("JSON deserialization error", e);
		}
	}

	public static <T> T parse(final InputStream stream, final Class<T> cls) {
		try {
			return mapper.readValue(stream, cls);
		} catch (IOException e) {
			throw new PdefException("JSON deserialization error", e);
		}
	}

	public static String serialize(final Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (IOException e) {
			throw new PdefException("JSON serialization error", e);
		}
	}

	public static void serialize(final Object o, final OutputStream stream) {
		try {
			mapper.writeValue(stream, o);
		} catch (IOException e) {
			throw new PdefException("JSON serialization error", e);
		}
	}

	private static class InternalModule extends Module {
		@Override
		public String getModuleName() {
			return "pdef-json";
		}

		@Override
		public Version version() {
			return Version.unknownVersion();
		}

		@Override
		public void setupModule(final SetupContext context) {
			context.addSerializers(new InternalSerializers());
			context.addDeserializers(new InternalDeserializers());
		}
	}

	private static class EnumSerializer extends StdScalarSerializer<Enum<?>> {
		EnumSerializer(final Class<Enum<?>> t) {
			super(t);
		}

		@Override
		public void serialize(final Enum<?> value, final JsonGenerator jgen,
				final SerializerProvider provider) throws IOException {
			if (value == null) {
				jgen.writeNull();
				return;
			}

			String s = value.name().toLowerCase();
			jgen.writeString(s);
		}
	}

	private static class EnumDeserializer extends StdScalarDeserializer<Enum<?>> {
		private final Method valueOf;

		EnumDeserializer(final Class<?> vc) {
			super(vc);

			try {
				valueOf = handledType().getDeclaredMethod("valueOf", String.class);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Enum<?> deserialize(final JsonParser jp, final DeserializationContext ctxt)
				throws IOException {
			String text = jp.getText().toUpperCase();
			boolean unknownAsNull = ctxt.getConfig()
					.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
			try {
				return (Enum<?>) valueOf.invoke(null, text);
			} catch (Exception e) {
				if (unknownAsNull) return null;

				throw new IOException(
						"Cannot deserialize enum " + handledType().getName() + " from " + text, e);
			}
		}
	}

	private static class InternalSerializers extends Serializers.Base {
		@SuppressWarnings("unchecked")
		@Override
		public JsonSerializer<?> findSerializer(final SerializationConfig config,
				final JavaType type, final BeanDescription beanDesc) {
			Class<?> cls = type.getRawClass();
			if (cls.isEnum()) {
				return new EnumSerializer((Class<Enum<?>>) cls);
			}

			return super.findSerializer(config, type, beanDesc);
		}
	}

	private static class InternalDeserializers extends Deserializers.Base {
		@Override
		public JsonDeserializer<?> findEnumDeserializer(final Class<?> type,
				final DeserializationConfig config, final BeanDescription beanDesc)
				throws JsonMappingException {
			return new EnumDeserializer(type);
		}
	}
}
