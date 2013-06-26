package io.pdef;

public interface Descriptor<T> {
	T getDefault();

	T parse(Object object);

	Object serialize(T object);
}
