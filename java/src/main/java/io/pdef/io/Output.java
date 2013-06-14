package io.pdef.io;

import java.util.List;

public interface Output {
	void write(boolean v);
	void write(short v);
	void write(int v);
	void write(long v);
	void write(float v);
	void write(double v);
	void write(String v);
	<T> void write(List<T> list, Writer.ListWriter<T> writer);
	<T> void write(T message, Writer.MessageWriter<T> writer);

	/** Double dispatch. */
	<T> void write(T object, Writer<T> writer);

	interface ListOutput {
		<T> void write(List<T> list, Writer<T> elementWriter);
	}

	interface MessageOutput {
		<T> void write(String field, T value, Writer<T> writer);
	}
}
