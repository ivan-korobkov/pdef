package io.pdef.invoke;

/** Combines success and exception invocation results. */
public class InvocationResult {
	private final Object data;
	private final RuntimeException exc;
	private final boolean ok;

	/** Creates a successful invocation result. */
	public static InvocationResult ok(final Object result) {
		return new InvocationResult(result, null, true);
	}

	/** Creates an exceptional invocation result. */
	public static InvocationResult exc(final RuntimeException exc) {
		return new InvocationResult(null, exc, false);
	}

	private InvocationResult(final Object data, final RuntimeException exc, final boolean ok) {
		this.data = data;
		this.exc = exc;
		this.ok = ok;
	}

	/** Returns a successul result data. */
	public Object getData() {
		return data;
	}

	/** Returns an exception. */
	public RuntimeException getExc() {
		return exc;
	}

	/** Returns true when a successful result, false when an exception. */
	public boolean isOk() {
		return ok;
	}
}
