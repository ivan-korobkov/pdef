package io.pdef;

public interface FieldAccessor<M, V> {
	/**
	 * Returns a field value in a message.
	 */
	V get(M message);

	/**
	 * Sets a field in a message.
	 */
	void set(M message, V value);
}
