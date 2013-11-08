package io.pdef.descriptors;

public interface ArgumentDescriptor<V> {
	/**
	 * Returns a method argument name.
	 */
	String getName();

	/**
	 * Returns an argument type descriptor.
	 */
	ValueDescriptor<V> getType();

	/**
	 * Returns whether this argument is an HTTP post argument.
	 */
	boolean isPost();

	/**
	 * Returns whether this argument is an HTTP query argument.
	 */
	boolean isQuery();

	/**
	 * Returns whether this argument is an HTTP query or post message form.
	 */
	boolean isForm();
}
