package pdef.generated;

import pdef.Message;

public abstract class GeneratedMessage implements Message {
	protected GeneratedMessage() {}

	protected GeneratedMessage(final Builder builder) {
		init(builder);
	}

	protected void init(final Builder builder) {}

	public static abstract class Builder implements Message.Builder {}
}
