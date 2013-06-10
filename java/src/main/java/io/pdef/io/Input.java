package io.pdef.io;

public interface Input {
	InputValue asValue();
	InputMessage asMessage();
	InputList asList();
}
