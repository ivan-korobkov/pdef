package io.pdef.io;

public interface InputValue extends Input {
	boolean getBoolean();
	short getShort();
	int getInt();
	long getLong();
	float getFloat();
	double getDouble();
	String getString();
}
