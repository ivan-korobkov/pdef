package io.pdef;

public interface Provider<T> {
	/**
	 * Returns an object.
	 */
	T get();
}
