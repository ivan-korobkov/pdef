package pdef;

import pdef.descriptors.MessageDescriptor;

import java.io.Serializable;
import java.util.Map;

public interface Message extends Serializable {
	/** Serializes this message to a map. */
	Map<String, Object> toMap();

	/** Serializes this message to a json string. */
	String toJson();

	/** Creates a builder and merges this message into it. */
	Builder toBuilder();

	/** Creates a new empty builder. */
	Builder builderForType();

	/** Returns a descriptor for this message type. */
	MessageDescriptor descriptorForType();

	public static interface Builder {
		/** Merges non-null fields from a message into this builder.
		 * This method is code-generated in subclasses for speed. */
		Builder merge(Message message);

		/** Builds an immutable message. */
		Message build();
	}
}
