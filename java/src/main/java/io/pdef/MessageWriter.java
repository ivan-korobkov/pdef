package io.pdef;

public interface MessageWriter<T> extends Writer<T> {
	void write(T message, MessageOutput output);
}
