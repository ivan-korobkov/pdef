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
}
