package io.pdef;

public interface Type<T> {
	T getDefault();

	T parse(Object object);

	Object serialize(T value);
}
