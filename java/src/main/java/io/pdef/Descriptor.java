package io.pdef;

public interface Descriptor<T> {
	Class<T> getJavaClass();

	T getDefault();

	T parse(Object object);

	Object serialize(T object);
}
