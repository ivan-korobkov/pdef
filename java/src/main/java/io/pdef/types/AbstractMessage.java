package io.pdef.types;

import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Map;

/** Abstract class for a generated message. */
public abstract class AbstractMessage implements Message, Serializable {
	protected AbstractMessage() {}

	@Override
	public Message copy() {
		return type().copy(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> toMap() {
		return (Map<String, Object>) type().toNative(this);
	}

	@Override
	public String toJson() {
		return type().toJson(this);
	}

	@Override
	public String toJson(final boolean indent) {
		return type().toJson(this, indent);
	}

	@Override
	public String toString() {
		Objects.ToStringHelper helper = Objects.toStringHelper(this);
		for (MessageField field : type().getFields()) {
			helper.add(field.getName(), field.get(this));
		}
		return helper.omitNullValues().toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractMessage cast = (AbstractMessage) o;
		for (MessageField field : type().getFields()) {
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
		for (MessageField field : type().getFields()) {
			Object value = field.get(this);
			result = 31 * result + (value != null ? value.hashCode() : 0);
		}

		return result;
	}
}
