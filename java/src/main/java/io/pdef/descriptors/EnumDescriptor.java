package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class EnumDescriptor extends AbstractDescriptor {
	private final Map<String, Enum<?>> values;

	public EnumDescriptor(final Class<?> enumClass, final DescriptorPool pool) {
		super(enumClass, DescriptorType.ENUM, pool);
		checkArgument(enumClass.isEnum());
		values = getValueMap(enumClass);
	}

	public Map<String, Enum<?>> getValues() {
		return values;
	}

	@Override
	protected void doLink() {}

	static Map<String, Enum<?>> getValueMap(final Class<?> type) {
		Enum<?>[] array = getValues(type);
		ImmutableMap.Builder<String, Enum<?>> builder = ImmutableMap.builder();
		for (Enum<?> value : array) {
			builder.put(value.name(), value);
		}
		return builder.build();
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
