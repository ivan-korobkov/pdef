package io.pdef;

import java.io.Serializable;

public interface Message extends Serializable {

	/** Copies this message to a new empty remote. */
	Builder toBuilder();

	/** Creates a new empty remote. */
	Builder builderForType();

	public static interface Builder {
		Message build();
	}

}
