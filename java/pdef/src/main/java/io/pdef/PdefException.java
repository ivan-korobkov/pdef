package io.pdef;

public class PdefException extends RuntimeException {
	public PdefException() {
		super();
	}

	public PdefException(final String s) {
		super(s);
	}

	public PdefException(final String s, final Throwable throwable) {
		super(s, throwable);
	}

	public PdefException(final Throwable throwable) {
		super(throwable);
	}
}
