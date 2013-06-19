package io.pdef;

public interface Writer<T> {
	void write(T value, Output output);
}
