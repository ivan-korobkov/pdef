package io.pdef.io;

public interface InputMessage extends Input {
	boolean getBoolean(String field);
	short getShort(String field);
	int getInt(String field);
	long getLong(String field);
	float getFloat(String field);
	double getDouble(String field);
	String getString(String field);
	InputValue getValue(String field);
	InputList getList(String field);
}
