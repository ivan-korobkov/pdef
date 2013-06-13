package io.pdef.io;

public interface InputMessage {
	boolean getBoolean(String field);
	short getShort(String field);
	int getInt(String field);
	long getLong(String field);
	float getFloat(String field);
	double getDouble(String field);
	String getString(String field);
	Input get(String field);
}
