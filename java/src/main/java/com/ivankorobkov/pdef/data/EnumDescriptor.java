package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public class EnumDescriptor<T extends Enum<T>> implements DataTypeDescriptor {

	private final TypeToken<T> type;
	private final ImmutableMap<String, T> values;

	public EnumDescriptor(final Class<T> type) {
		this(TypeToken.of(type));
	}

	public EnumDescriptor(final TypeToken<T> type) {
		this.type = checkNotNull(type);

		@SuppressWarnings("unchecked")
		Class<T> cls = (Class<T>) type.getRawType();
		T[] array = getEnumValues(cls);

		ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
		for (T t : array) {
			builder.put(t.name(), t);
		}
		values = builder.build();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.toString();
	}

	@Override
	public String getName() {
		return "Enum";
	}

	public TypeToken<T> getType() {
		return type;
	}

	@Override
	public List<TypeVariable<?>> getVariables() {
		return ImmutableList.of();
	}

	@Override
	public DataTypeDescriptor parameterize(final TypeToken<?> parameterizedType) {
		return this;
	}

	@Override
	public Object merge(@Nullable final Object object, @Nullable final Object another) {
		if (another == null) {
			return object;
		}

		return another;
	}

	@Override
	public Object deepCopy(@Nullable final Object object) {
		return object;
	}

	@Override
	public void link(final DescriptorPool pool) {}

	public Map<String, T> getValues() {
		return values;
	}

	static <T extends Enum<T>> T[] getEnumValues(final Class<T> type) {
		try {
			Method method = type.getMethod("values");
			@SuppressWarnings("unchecked")
			T[] array = (T[]) method.invoke(null);
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
