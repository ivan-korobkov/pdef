package io.pdef;

import com.google.common.base.Objects;
import io.pdef.descriptors.FieldDescriptor;

import java.io.Serializable;
import java.util.Map;

/** Abstract class for a generated exception. */
public abstract class GeneratedException extends RuntimeException implements Message, Serializable {
	private transient int cachedHash;

	protected GeneratedException() {}
	protected GeneratedException(final Builder builder) {}

	@Override
	public Map<String, Object> toMap() {
		return descriptorForType().toObject(this);
	}

	@Override
	public Message.Builder toBuilder() {
		return descriptorForType().toBuilder(this);
	}

	@Override
	public String toJson() {
		return descriptorForType().toJson(this);
	}

	@Override
	public String toJson(boolean indent) {
		return descriptorForType().toJson(this, indent);
	}

	@Override
	public String toString() {
		Objects.ToStringHelper helper = Objects.toStringHelper(this);
		for (FieldDescriptor field : descriptorForType().getFields()) {
			helper.add(field.getName(), field.get(this));
		}
		return helper.omitNullValues().toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GeneratedException cast = (GeneratedException) o;
		for (FieldDescriptor field : descriptorForType().getFields()) {
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
		if (cachedHash != 0) return cachedHash;

		int result = 0;
		for (FieldDescriptor field : descriptorForType().getFields()) {
			Object value = field.get(this);
			result = 31 * result + (value != null ? value.hashCode() : 0);
		}

		return cachedHash = result;
	}

	public static abstract class Builder implements Message.Builder {
		protected Builder() {}
		protected Builder(final GeneratedMessage message) {}

		@Override
		public Message.Builder merge(final Message message) {
			return this;
		}

		@Override
		public boolean equals(final Object o) {
			return this == o || !(o == null || getClass() != o.getClass());
		}

		@Override
		public int hashCode() {
			return 31;
		}
	}
}
