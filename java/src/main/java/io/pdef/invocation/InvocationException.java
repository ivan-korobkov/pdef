package io.pdef.invocation;

public class InvocationException extends RuntimeException {

	public InvocationException() {
	}

	public InvocationException(final String message) {
		super(message);
	}

	public InvocationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public InvocationException(final Throwable cause) {
		super(cause);
	}
}
