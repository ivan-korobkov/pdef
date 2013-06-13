package io.pdef.io;

public interface Input {
	boolean isNull();
	InputValue asValue();
	InputMessage asMessage();
	InputList asList();
}
