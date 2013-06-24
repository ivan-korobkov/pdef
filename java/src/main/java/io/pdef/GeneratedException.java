package io.pdef;

import java.io.Serializable;

public abstract class GeneratedException extends RuntimeException implements Message, Serializable {
	private transient int hash;

	protected GeneratedException() {}

	protected GeneratedException(final Builder builder) {}

	protected GeneratedException(final MessageInput input) {}

	protected void write(final MessageOutput output) {}

	public abstract Builder toBuilder();
	public abstract Builder builderForType();

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
