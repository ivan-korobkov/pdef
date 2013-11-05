package io.pdef;

import java.util.Map;

public interface Message {
	/**
	 * Returns a deep copy of this message.
	 */
	Message copy();

	/**
	 * Returns this message descriptor.
	 */
	MessageDescriptor<? extends Message> descriptor();
	
	/**
	 * Serializes this message to a map.
	 */
	Map<String, Object> toMap();

	/**
	 * Serializes this message to a JSON string without indentation.
	 */
	String toJson();

	/**
	 * Serializes this method to a JSON string with optional indentation.
	 */
	String toJson(boolean indent);
}
