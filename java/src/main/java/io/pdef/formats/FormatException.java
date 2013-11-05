package io.pdef.formats;

/**
 * FormatException wraps all parsing/serialization exceptions.
 * */
public class FormatException extends RuntimeException {
	public FormatException() {}

	public FormatException(final String s) {
		super(s);
	}

	public FormatException(final String s, final Throwable throwable) {
		super(s, throwable);
	}

	public FormatException(final Throwable throwable) {
		super(throwable);
	}
}
