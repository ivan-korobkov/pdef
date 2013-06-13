package io.pdef;

import java.io.Serializable;

public interface Message extends Serializable {

	/** Copies this message to a new empty builder. */
	Builder toBuilder();

	/** Creates a new empty builder. */
	Builder builderForType();

	public static interface Builder {
		Message build();
	}
}
