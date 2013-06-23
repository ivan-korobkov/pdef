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
	<T> void write(List<T> list, Writer<T> elementWriter);
	<T> void write(T message, Message.MessageWriter<T> writer);
	<T> void write(T object, Writer<T> writer);

}
