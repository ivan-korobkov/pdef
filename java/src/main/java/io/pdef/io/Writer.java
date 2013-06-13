package io.pdef.io;

public interface Writer<T> {
	void write(T object, Output output);
}
