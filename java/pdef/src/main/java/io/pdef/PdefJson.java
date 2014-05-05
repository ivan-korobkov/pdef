package io.pdef;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PdefJson {
	private static final Gson gson;

	static {
		gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
				.setExclusionStrategies(new ExceptionExclusionStrategy())
				.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
				.create();
	}

	public static Object parse(final String s, final Type type) {
		return gson.fromJson(s, type);
	}

	public static <T> T parse(final String s, final Class<T> cls) {
		return gson.fromJson(s, cls);
	}

	public static <T> T parse(final Reader reader, final Class<T> cls) {
		return gson.fromJson(reader, cls);
	}

	public static <T> T parse(final InputStream stream, final Class<T> cls) {
		return gson.fromJson(new InputStreamReader(stream), cls);
	}

	public static String serialize(final Object o) {
		return gson.toJson(o);
	}

	public static void serialize(final Object o, final Writer writer) {
		gson.toJson(o, writer);
	}

	public static void serialize(final Object o, final OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		gson.toJson(o, writer);
		
		try {
			writer.flush();
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
	}

	/** Excludes fields from java exception classes. */
	private static class ExceptionExclusionStrategy implements ExclusionStrategy {
		@Override
		public boolean shouldSkipField(final FieldAttributes f) {
			Class<?> cls = f.getDeclaringClass();
			return cls == RuntimeException.class || cls == Exception.class
					|| cls == Throwable.class || cls == Error.class;
		}

		@Override
		public boolean shouldSkipClass(final Class<?> clazz) {
			return false;
		}
	}

	private static class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			@SuppressWarnings("unchecked")
			Class<T> rawType = (Class<T>) type.getRawType();
			if (!rawType.isEnum()) {
				return null;
			}

			return new EnumTypeAdapter<T>(rawType);
		}
	}

	private static class EnumTypeAdapter<T> extends TypeAdapter<T> {
		private final Map<String, T> lowercaseToConstant;

		private EnumTypeAdapter(final Class<T> enumType) {
			lowercaseToConstant = new HashMap<String, T>();

			for (T constant : enumType.getEnumConstants()) {
				lowercaseToConstant.put(toLowercase(constant), constant);
			}
		}

		public void write(JsonWriter out, T value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(toLowercase(value));
			}
		}

		public T read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			} else {
				return lowercaseToConstant.get(reader.nextString());
			}
		}

		private String toLowercase(Object o) {
			return o.toString().toLowerCase(Locale.US);
		}
	}
}
