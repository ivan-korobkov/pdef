package com.ivankorobkov.pdef.data;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.List;

public class TypeVariableDescriptor<T extends TypeVariable> implements DataTypeDescriptor {

	private final TypeToken<T> type;

	public TypeVariableDescriptor(final TypeToken<T> type) {
		this.type = checkNotNull(type);
	}

	@Override
	public String getName() {
		return type.getRawType().getSimpleName();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public Object deepCopy(@Nullable final Object object) {
		return object;
	}

	@Override
	public void link(final DescriptorPool pool) {
		// Do nothing.
	}
}
