package io.pdef;

import java.io.Serializable;

public abstract class GeneratedMessage implements Message, Serializable {
	private transient int hash;
	protected GeneratedMessage(final Builder builder) {}

	public abstract Builder toBuilder();

	public abstract Builder newEmptyBuilder();

	@Override
	public boolean equals(final Object o) {
		return this == o || !(o == null || getClass() != o.getClass());
	}

	@Override
	public int hashCode() {
		if (hash == 0) hash = generateHashCode();
		return hash;
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
