package io.pdef.io;

import java.util.List;

public interface Writer<T> {
	void write(T value, Output output);

	interface ListWriter<T> extends Writer<List<T>> {
		void write(List<T> list, Output.ListOutput output);
	}

	interface MessageWriter<T> extends Writer<T> {
		void write(T message, Output.MessageOutput output);
	}
}
