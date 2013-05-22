package io.pdef.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class CaseInsensitiveEnumDeserializer extends StdScalarDeserializer<Enum<?>> {
	private final Map<String, Enum<?>> map;

	public CaseInsensitiveEnumDeserializer(final Class<Enum<?>> cls) {
		super(cls);

		ImmutableMap.Builder<String, Enum<?>> builder = ImmutableMap.builder();
		for (Enum<?> anEnum : getValues(cls)) {
			builder.put(anEnum.name(), anEnum);
		}
		map = builder.build();
	}

	@Override
	public Enum<?> deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException {
		String text = jp.getText().toUpperCase();
		boolean unknownAsNull = ctxt.getConfig()
				.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

		Enum<?> e = map.get(text);
		if (e != null || unknownAsNull) return e;

		throw new IOException(
				"Cannot deserialize enum " + getValueClass().getName() + " from \"" + text + "\"");
	}

	static Enum<?>[] getValues(final Class<?> type) {
		try {
			Method method = type.getMethod("values");
			@SuppressWarnings("unchecked")
			Enum<?>[] array = (Enum<?>[]) method.invoke(null);
			return array;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
