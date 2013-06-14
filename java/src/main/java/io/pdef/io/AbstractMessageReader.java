package io.pdef.io;

public abstract class AbstractMessageReader<T> implements Reader.MessageReader<T> {
	@Override
	public T get(final Input input) {
		return input.getMessage(this);
	}
}
