package io.pdef;

import java.io.Serializable;

public interface Message extends Serializable {

	/** Copies this message to a new empty remote. */
	Builder toBuilder();

	/** Creates a new empty remote. */
	Builder builderForType();

	public static interface Builder {
		Message build();
	}

	interface MessageInput {
		<T> T get(String field, Reader<T> reader);
	}

	interface MessageOutput {
		<T> void write(String field, T value, Writer<T> writer);
	}

	interface MessageReader<T> extends Reader<T> {
		T get(MessageInput input);
	}

	interface MessageWriter<T> extends Writer<T> {
		void write(T message, MessageOutput output);
	}
}
