package io.pdef;

public class PdefClientException extends PdefException {
	public PdefClientException() {
		super();
	}

	public PdefClientException(final String s) {
		super(s);
	}

	public PdefClientException(final String s, final Throwable throwable) {
		super(s, throwable);
	}

	public PdefClientException(final Throwable throwable) {
		super(throwable);
	}
}
