package io.pdef;

public abstract class GeneratedException extends RuntimeException implements Message {
	protected GeneratedException(final Builder builder) {}

	public Builder toBuilder() {
		Builder builder = newBuilderForType();
		fill(builder);
		return builder;
	}

	protected void fill(Builder builder) {}

	public abstract Builder newBuilderForType();

	public static abstract class Builder implements Message.Builder {}
}
