package pdef.formats;

public class FormatException extends RuntimeException {
	public FormatException() {}

	public FormatException(final String message) {
		super(message);
	}

	public FormatException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public FormatException(final Throwable cause) {
		super(cause);
	}
}
