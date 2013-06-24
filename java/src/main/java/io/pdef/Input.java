package io.pdef;

import java.util.List;

public interface Input {
	boolean readBoolean();

	short readShort();

	int readInt();

	long readLong();

	float readFloat();

	double readDouble();

	String readString();

	Object readObject();

	<T> List<T> readList(Reader<T> elementReader);

	<T> T readMessage(MessageReader<T> reader);

	<T> T read(Reader<T> reader);
}
