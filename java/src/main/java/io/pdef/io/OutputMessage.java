package io.pdef.io;

public interface OutputMessage {
	void write(String field, boolean v);
	void write(String field, short v);
	void write(String field, int v);
	void write(String field, long v);
	void write(String field, float v);
	void write(String field, double v);
	void write(String field, String v);
	Output write(String field);
}
