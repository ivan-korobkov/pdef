package io.pdef;

public abstract class GeneratedMessage implements Message {
	protected GeneratedMessage(final Builder builder) {}

	public Builder toBuilder() {
		Builder builder = newBuilderForType();
		fill(builder);
		return builder;
	}

	protected void fill(Builder builder) {}

	public abstract Builder newBuilderForType();

	@Override
	public boolean equals(final Object o) {
		return this == o || !(o == null || getClass() != o.getClass());
	}

	@Override
	public int hashCode() {
		return 31;
	}

	public static abstract class Builder implements Message.Builder {
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
