package pdef;

import pdef.descriptors.MessageDescriptor;

import java.io.Serializable;
import java.util.Map;

public abstract class GeneratedException extends RuntimeException implements Message, Serializable {
	private transient int cachedHash;

	protected GeneratedException() {}
	protected GeneratedException(final Builder builder) {}

	@Override
	public Map<String, Object> toMap() {
		MessageDescriptor descriptor = descriptorForType();
		return descriptor.toObject(this);
	}

	@Override
	public String toJson() {
		MessageDescriptor descriptor = descriptorForType();
		return descriptor.toJson(this);
	}

	@Override
	public boolean equals(final Object o) {
		return this == o || !(o == null || getClass() != o.getClass());
	}

	@Override
	public int hashCode() {
		if (cachedHash == 0) cachedHash = generateHashCode();
		return cachedHash;
	}

	protected int generateHashCode() {
		return 31;
	}

	public static abstract class Builder implements Message.Builder {
		public Builder() {}
		public Builder(final GeneratedException message) {}

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
