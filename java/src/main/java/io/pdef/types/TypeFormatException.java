package io.pdef.types;

/**
 * TypeFormatException wraps all parsing/serialization exceptions.
 * */
public class TypeFormatException extends RuntimeException {
	public TypeFormatException() {}

	public TypeFormatException(final String s) {
		super(s);
	}

	public TypeFormatException(final String s, final Throwable throwable) {
		super(s, throwable);
	}

	public TypeFormatException(final Throwable throwable) {
		super(throwable);
	}
}
