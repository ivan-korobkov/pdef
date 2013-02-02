package com.ivankorobkov.pdef.data;

import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.GenericDescriptor;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.List;

public interface DataTypeDescriptor extends GenericDescriptor {

	List<TypeVariable<?>> getVariables();

	DataTypeDescriptor parameterize(TypeToken<?> parameterizedType);

	Object merge(@Nullable Object object, @Nullable Object another);

	Object deepCopy(@Nullable Object object);
}
