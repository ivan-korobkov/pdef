package io.pdef;

public class SerializationException extends RuntimeException {
	public SerializationException() {}

	public SerializationException(final String message) {
		super(message);
	}

	public SerializationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public SerializationException(final Throwable cause) {
		super(cause);
	}
}
