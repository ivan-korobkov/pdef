package io.pdef;

public interface ArgumentDescriptor<V> {
	/**
	 * Returns a method argument name.
	 */
	String getName();

	/**
	 * Returns an argument type descriptor.
	 */
	DataTypeDescriptor<V> getType();
}
