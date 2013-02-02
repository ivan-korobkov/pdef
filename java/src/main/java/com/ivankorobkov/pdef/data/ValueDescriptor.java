package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.List;

public final class ValueDescriptor<T> implements DataTypeDescriptor {

	private final String name;
	private final Class<T> type;

	public ValueDescriptor(final String name, final Class<T> type) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
	}

	public static <T> ValueDescriptor<T> create(final String name, final Class<T> type) {
		return new ValueDescriptor<T>(name, type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.addValue(type)
				.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	public Class<T> getType() {
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
	public void link(final DescriptorPool pool) {
		// Do nothing.
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
}
