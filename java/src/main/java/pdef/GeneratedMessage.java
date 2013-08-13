package pdef;

import pdef.descriptors.MessageDescriptor;

import java.util.Map;

/** Abstract class for a generated message. */
public abstract class GeneratedMessage implements Message {
	private transient int cachedHash;

	protected GeneratedMessage() {}
	protected GeneratedMessage(final Builder builder) {}

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
		protected Builder() {}
		protected Builder(final GeneratedMessage message) {}

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
