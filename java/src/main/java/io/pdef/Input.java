package io.pdef;

import java.util.List;

public interface Input {
	boolean getBoolean();
	short getShort();
	int getInt();
	long getLong();
	float getFloat();
	double getDouble();
	String getString();
	<T> List<T> getList(Reader<T> elementReader);
	<T> T getMessage(MessageReader<T> reader);
	<T> T get(Reader<T> reader);
}
