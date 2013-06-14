package io.pdef.io;

import java.util.List;

public interface Reader<T> {
	T get(Input input);

	interface ListReader<T> extends Reader<List<T>> {
		List<T> get(Input.ListInput input);
	}

	interface MessageReader<T> extends Reader<T> {
		T get(Input.MessageInput input);
	}
}
