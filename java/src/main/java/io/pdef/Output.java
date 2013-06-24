package io.pdef;

import java.util.List;

public interface Output {
	void write(boolean v);

	void write(short v);

	void write(int v);

	void write(long v);

	void write(float v);

	void write(double v);

	void write(String v);

	void writeNull();

	void writeObject(Object value);

	<T> void writeList(List<T> list, Writer<T> elementWriter);

	<T> void writeMessage(T message, MessageWriter<T> writer);

	<T> void write(T object, Writer<T> writer);
}
