package io.pdef;

public interface DataDescriptor<T> extends Descriptor<T> {
	/**
	 * Returns a deep copy of an object.
	 */
	T copy(T object);
}
