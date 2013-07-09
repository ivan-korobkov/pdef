package io.pdef;

/** Data type descriptor. */
public interface Descriptor<T> {
	Class<T> getJavaClass();

	T getDefault();

	T parse(Object object);

	Object serialize(T object);
}
