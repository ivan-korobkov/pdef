package pdef.generated;

import pdef.Message;
import pdef.Type;

public abstract class GeneratedMessage implements Message {
	private volatile boolean initedDefaultFields;

	protected GeneratedMessage(final Builder builder) {}

	protected void initDefaultFields() {
		if (initedDefaultFields) {
			return;
		}

		synchronized (Type.class) {
			if (initedDefaultFields) {
				return;
			}

			initedDefaultFields = true;
			doInitDefaultFields();
		}
	}

	protected void doInitDefaultFields() {}

	public static abstract class Builder implements Message.Builder {}
}
