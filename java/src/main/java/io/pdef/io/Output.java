package io.pdef.io;

public interface Output {
	void writeNull();
	OutputValue asValue();
	OutputMessage asMessage();
	OutputList asList();
}
