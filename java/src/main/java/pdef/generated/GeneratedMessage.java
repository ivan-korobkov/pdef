package pdef.generated;

import pdef.Message;

import java.util.BitSet;

public abstract class GeneratedMessage implements Message {
	protected final BitSet _fields = new BitSet();

	protected GeneratedMessage(final Builder builder) {
		_fields.and(builder._fields);
	}

	public static abstract class Builder implements Message.Builder {
		protected final BitSet _fields = new BitSet();
	}
}
