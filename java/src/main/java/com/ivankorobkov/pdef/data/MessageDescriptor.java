package com.ivankorobkov.pdef.data;

import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface MessageDescriptor extends DataTypeDescriptor {

	Message createInstance();

	boolean isLineFormat();

	// Fields

	List<MessageField> getFields();

	Map<String, MessageField> getFieldMap();

	List<MessageField> getDeclaredFields();

	// Subtypes

	boolean isTypeBase();

	MessageField getTypeBaseField();

	Map<Enum<?>, TypeToken<?>> getSubtypeTokenMap();

	Map<Enum<?>, MessageDescriptor> getSubtypeMap();

	// Parameterization

	DataTypeDescriptor parameterize(TypeToken<?> parameterizedType);

	// Instance operations

	@Override
	Message merge(Object object, Object another);

	@Override
	Message deepCopy(@Nullable Object object);
}
