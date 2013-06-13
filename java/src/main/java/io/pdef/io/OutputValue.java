package io.pdef.io;

public interface OutputValue {
	void write(boolean v);
	void write(short v);
	void write(int v);
	void write(long v);
	void write(float v);
	void write(double v);
	void write(String v);
}
