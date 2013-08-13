package pdef;

import pdef.descriptors.MessageDescriptor;

import java.io.Serializable;
import java.util.Map;

public interface Message extends Serializable {
	/** Serializes this message to a map. */
	Map<String, Object> toMap();

	/** Serializes this message to a json string. */
	String toJson();

	/** Copies this message to a new builder. */
	Builder toBuilder();

	/** Creates a new empty builder. */
	Builder builderForType();

	/** Returns a descriptor for this message type. */
	MessageDescriptor descriptorForType();

	public static interface Builder {
		Message build();
	}
}
