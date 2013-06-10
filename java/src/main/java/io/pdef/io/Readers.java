package io.pdef.io;

public class Readers {
	private Readers() {}

	public static Reader<String> stringReader() {
		return new Reader<String>() {
			@Override
			public String read(final Input input) {
				return input.asValue().getString();
			}
		};
	}
}
