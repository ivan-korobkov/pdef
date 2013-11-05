package io.pdef;

public interface Descriptor<T> {
	/**
	 * Returns a pdef type.
	 */
	TypeEnum getType();

	/**
	 * Returns a java class.
	 */
	Class<T> getJavaClass();
}
