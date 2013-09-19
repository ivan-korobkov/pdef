package pdef.invocation;

/** Combines success and exception invocation results. */
public class InvocationResult {
	private final Object result;
	private final boolean ok;

	/** Creates a successful invocation result. */
	public static InvocationResult ok(final Object result) {
		return new InvocationResult(result, true);
	}

	/** Creates an exceptional invocation result. */
	public static InvocationResult exc(final RuntimeException exception) {
		return new InvocationResult(exception, false);
	}

	private InvocationResult(final Object result,
			final boolean ok) {
		this.result = result;
		this.ok = ok;
	}

	/** Returns a result or throws IllegalStateException when not a successful invocation. */
	public Object getData() {
		return result;
	}

	/** Returns true when a successful result, false when an exception. */
	public boolean isOk() {
		return ok;
	}
}
