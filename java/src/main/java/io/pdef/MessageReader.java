package io.pdef;

public interface MessageReader<T> extends Reader<T> {
	T get(MessageInput input);
}
