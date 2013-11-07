package io.pdef;

import io.pdef.formats.JsonFormat;
import io.pdef.formats.NativeFormat;

import java.io.Serializable;
import java.util.Map;

/**
 * Abstract class for a generated Pdef message.
 * */
public abstract class AbstractMessage implements Message, Serializable {
	protected AbstractMessage() {}

	protected AbstractMessage(final AbstractMessage another) {}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> toMap() {
		return (Map<String, Object>) NativeFormat.instance().serialize(this, uncheckedDescriptor());
	}

	@Override
	public String toJson() {
		return toJson(true);
	}

	@Override
	public String toJson(final boolean indent) {
		return JsonFormat.instance().serialize(this, uncheckedDescriptor(), indent);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append('{');

		String nextSeparator = "";
		MessageDescriptor<Message> descriptor = uncheckedDescriptor();
		for (FieldDescriptor<? super Message, ?> field : descriptor.getFields()) {
			Object value = field.get(this);
			if (value == null) {
				continue;
			}

			sb.append(nextSeparator);
			sb.append(field.getName()).append('=').append(value);
			nextSeparator = ", ";
		}

		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractMessage cast = (AbstractMessage) o;
		MessageDescriptor<Message> descriptor = uncheckedDescriptor();
		for (FieldDescriptor<? super Message, ?> field : descriptor.getFields()) {
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

		MessageDescriptor<Message> descriptor = uncheckedDescriptor();
		for (FieldDescriptor<? super Message, ?> field : descriptor.getFields()) {
			Object value = field.get(this);
			result = 31 * result + (value == null ? 0 : value.hashCode());
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private MessageDescriptor<Message> uncheckedDescriptor() {
		return (MessageDescriptor<Message>) descriptor();
	}
}
