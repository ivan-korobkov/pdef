package io.pdef;

public interface MessageOutput {
	<T> void write(String field, T value, Writer<T> writer);
}
