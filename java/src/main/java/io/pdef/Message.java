package io.pdef;

import java.io.Serializable;

public interface Message extends Serializable {

	/** Copies this message to a new empty remote. */
	Builder toBuilder();

	/** Creates a new empty builder. */
	Builder builderForClass();

	/** Returns a type for this message class. */
	Type<? extends Message> typeForClass();

	public static interface Builder {
		Message build();
	}
}
