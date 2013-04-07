package pdef.rpc;

public class DispatcherException extends RuntimeException {

	public DispatcherException() {
	}

	public DispatcherException(final String message) {
		super(message);
	}

	public DispatcherException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DispatcherException(final Throwable cause) {
		super(cause);
	}

	public DispatcherException(final String message, final Throwable cause,
			final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
