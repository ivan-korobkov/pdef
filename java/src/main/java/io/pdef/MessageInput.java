package io.pdef;

public interface MessageInput {
	<T> T get(String field, Reader<T> reader);
}
