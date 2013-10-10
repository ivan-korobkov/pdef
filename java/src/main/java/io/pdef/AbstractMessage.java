package io.pdef;

import com.google.common.base.Objects;
import io.pdef.format.JsonFormat;
import io.pdef.format.NativeFormat;
import io.pdef.meta.MessageField;
import io.pdef.meta.MessageType;

import java.io.Serializable;
import java.util.Map;

/**
 * Abstract class for a generated Pdef message.
 * */
public abstract class AbstractMessage implements Message, Serializable {
	protected AbstractMessage() {}

	@SuppressWarnings("unchecked")
	private MessageType<Message> uncheckedType() {
		return (MessageType<Message>) metaType();
	}

	@Override
	public Message copy() {
		return uncheckedType().copy(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> serializeToMap() {
		return (Map<String, Object>) NativeFormat.instance().serialize(uncheckedType(), this);
	}

	@Override
	public String serializeToJson() {
		return JsonFormat.instance().serialize(uncheckedType(), this);
	}

	@Override
	public String serializeToJson(final boolean indent) {
		return JsonFormat.instance().serialize(uncheckedType(), this, indent);
	}

	@Override
	public String toString() {
		Objects.ToStringHelper helper = Objects.toStringHelper(this);

		MessageType<Message> type = uncheckedType();
		for (MessageField<? super Message, ?> field : type.getFields()) {
			helper.add(field.getName(), field.get(this));
		}

		return helper.omitNullValues().toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractMessage cast = (AbstractMessage) o;
		MessageType<Message> type = uncheckedType();
		for (MessageField<? super Message, ?> field : type.getFields()) {
			Object value0 = field.get(this);
			Object value1 = field.get(cast);
			if (value0 != null ? !value0.equals(value1) : value1 != null) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;

		MessageType<Message> type = uncheckedType();
		for (MessageField<? super Message, ?> field : type.getFields()) {
			Object value = field.get(this);
			result = 31 * result + (value == null ? 0 : value.hashCode());
		}

		return result;
	}
}
