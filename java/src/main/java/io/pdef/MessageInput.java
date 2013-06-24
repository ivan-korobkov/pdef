package io.pdef;

public interface MessageInput {
	<T> T read(String field, Reader<T> reader);
}
