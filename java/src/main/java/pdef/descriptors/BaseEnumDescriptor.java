package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class BaseEnumDescriptor implements EnumDescriptor {
	private final Class<?> javaClass;
	private final Map<String, Enum<?>> values;

	public BaseEnumDescriptor(final Class<Enum<?>> javaClass) {
		this.javaClass = checkNotNull(javaClass);
		values = getValueMap(javaClass);
	}

	@Override
	public Class<?> getJavaClass() { return javaClass; }

	@Override
	public Map<String, Enum<?>> getValues() { return values; }

	@Override
	public void link() {}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	static Map<String, Enum<?>> getValueMap(final Class<Enum<?>> type) {
		Enum<?>[] array = getValues(type);
		ImmutableMap.Builder<String, Enum<?>> builder = ImmutableMap.builder();
		for (Enum<?> value : array) {
			builder.put(value.name(), value);
		}
		return builder.build();
	}

	static Enum<?>[] getValues(final Class<Enum<?>> type) {
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
