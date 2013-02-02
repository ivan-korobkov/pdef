package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.ivankorobkov.pdef.DescriptorPool;

import javax.annotation.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

public abstract class AbstractMessageDescriptor extends AbstractDataTypeDescriptor
		implements MessageDescriptor {

	private final TypeToken<?> type;
	private ImmutableMap<String, MessageField> fieldMap;
	private ImmutableMap<Enum<?>, MessageDescriptor> subtypeMap;

	public AbstractMessageDescriptor(final TypeToken<?> type,
			final Map<TypeVariable<?>, TypeToken<?>> argMap) {
		super(type, argMap);

		this.type = type;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("MessageDescriptor").
				addValue(getName()).
				addValue(type).
				toString();
	}

	@Override
	public List<MessageField> getFields() {
		return ImmutableList.of();
	}

	@Override
	public Map<String, MessageField> getFieldMap() {
		return fieldMap;
	}

	@Override
	public List<MessageField> getDeclaredFields() {
		return ImmutableList.of();
	}

	@Override
	public Map<Enum<?>, MessageDescriptor> getSubtypeMap() {
		return subtypeMap;
	}

	@Override
	public void link(final DescriptorPool pool) {
		// Link the fields.
		for (MessageField field : getFields()) {
			field.link(pool);
		}

		ImmutableMap.Builder<String, MessageField> fieldBuilder = ImmutableMap.builder();
		for (MessageField field : getFields()) {
			String name = field.getName();
			fieldBuilder.put(name, field);
		}
		fieldMap = fieldBuilder.build();

		// Link the subtypes.
		ImmutableMap.Builder<Enum<?>, MessageDescriptor> subtypeBuilder = ImmutableMap.builder();
		for (Map.Entry<Enum<?>, TypeToken<?>> e : getSubtypeTokenMap().entrySet()) {
			TypeToken<?> token = e.getValue();
			MessageDescriptor descriptor = (MessageDescriptor) pool.get(token);

			subtypeBuilder.put(e.getKey(), descriptor);
		}
		subtypeMap = subtypeBuilder.build();
	}

	@Override
	public Message merge(final Object current, final Object another) {
		Message currentMessage = (Message) current;
		if (another == null) {
			return currentMessage;
		}

		Message anotherMessage = (Message) another;
		MessageDescriptor anotherDescriptor = anotherMessage.getDescriptor();
		if (currentMessage == null) {
			currentMessage = anotherDescriptor.createInstance();
		}

		for (MessageField field : anotherDescriptor.getFields()) {
			if (field.isReadOnly() || !field.isSetIn(anotherMessage)) {
				continue;
			}

			field.getDescriptor();
			field.merge(currentMessage, anotherMessage);
		}

		return currentMessage;
	}

	@Override
	public Message deepCopy(@Nullable final Object object) {
		if (object == null) {
			return null;
		}

		Message copy = createInstance();
		return merge(copy, object);
	}
}
