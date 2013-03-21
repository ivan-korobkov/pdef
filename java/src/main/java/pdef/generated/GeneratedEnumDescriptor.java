package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import pdef.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GeneratedEnumDescriptor implements EnumDescriptor, GeneratedDescriptor {
	private final Class<?> javaClass;
	private final Map<String, Enum<?>> values;

	public GeneratedEnumDescriptor(final Class<?> javaClass) {
		this.javaClass = checkNotNull(javaClass);
		values = getValueMap(javaClass);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(javaClass)
				.toString();
	}

	@Override
	public Map<String, Enum<?>> getValues() { return values; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

	@Override
	public TypeDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	@Override
	public void initialize() {}

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
