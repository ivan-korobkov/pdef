package io.pdef;

import io.pdef.descriptors.Descriptor;

import java.io.Serializable;
import java.util.Map;

public interface Message extends Serializable {
	/** Serializes this message to a map. */
	Map<String, Object> serialize();

	/** Serializes this message to a JSON string. */
	String serializeToJson();

	/** Copies this message to a new empty builder. */
	Builder toBuilder();

	/** Creates a new empty builder. */
	Builder builderForType();

	/** Returns a descriptor for this message type. */
	Descriptor<? extends Message> descriptorForType();

	public static interface Builder {
		Message build();
	}
}
