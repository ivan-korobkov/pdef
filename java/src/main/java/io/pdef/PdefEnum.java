package io.pdef;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/** Pdef enum descriptor. */
public class PdefEnum extends PdefDescriptor {
	private final BiMap<String, Enum<?>> values;

	PdefEnum(final Type javaType, final Pdef pdef) {
		super(PdefType.ENUM, javaType, pdef);

		ImmutableBiMap.Builder<String, Enum<?>> builder = ImmutableBiMap.builder();
		for (Enum<?> anEnum : enumValues(javaType)) {
			builder.put(anEnum.name(), anEnum);
		}
		values = builder.build();
	}

	/** Returns an immutable bidirectional map of enum names and values. */
	public BiMap<String, Enum<?>> getValues() {
		return values;
	}

	@SuppressWarnings("unchecked")
	static Enum<?>[] enumValues(final Type javaType) {
		Class<?> cls = (Class<?>) javaType;
		try {
			Method method = cls.getMethod("values");
			return (Enum<?>[]) method.invoke(null);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
