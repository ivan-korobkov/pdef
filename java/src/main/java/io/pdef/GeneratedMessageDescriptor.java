package io.pdef;

public abstract class GeneratedMessageDescriptor<T>
		implements Descriptor<T>, MessageReader<T>, MessageWriter<T> {
	private final T instance;

	protected GeneratedMessageDescriptor(final T instance) {
		this.instance = instance;
	}

	@Override
	public T getDefault() {
		return instance;
	}

	@Override
	public T read(final Input input) {
		return input.readMessage(this);
	}

	@Override
	public void write(final T value, final Output output) {
		output.writeMessage(value, this);
	}
}
