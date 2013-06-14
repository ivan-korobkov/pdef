package io.pdef.io;

import java.util.List;

public interface Input {
	boolean getBoolean();
	short getShort();
	int getInt();
	long getLong();
	float getFloat();
	double getDouble();
	String getString();
	<T> List<T> getList(Reader.ListReader<T> reader);
	<T> T getMessage(Reader.MessageReader<T> reader);

	/** Double dispatch. */
	<T> T get(Reader<T> reader);

	interface ListInput {
		<T> List<T> get(Reader<T> elementReader);
	}

	interface MessageInput {
		<T> T get(String field, Reader<T> reader);
	}
}
