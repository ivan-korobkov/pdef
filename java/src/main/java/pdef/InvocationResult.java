package pdef;

import com.google.common.base.Objects;
import pdef.descriptors.Descriptor;
import pdef.descriptors.MethodDescriptor;

/** Immutable invocation result of a method. */
public class InvocationResult {
	private final Object result;
	private final boolean success;
	private final MethodDescriptor method;

	public static InvocationResult success(final Object result,
			final MethodDescriptor method) {
		return new InvocationResult(result, true, method);
	}

	public static InvocationResult exc(final Object result,
			final MethodDescriptor method) {
		return new InvocationResult(result, false, method);
	}

	private InvocationResult(final Object result, final boolean success,
			final MethodDescriptor method) {
		this.result = result;
		this.success = success;
		this.method = method;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(result)
				.addValue(method)
				.toString();
	}

	public Object getResult() {
		return result;
	}

	public boolean isSuccess() {
		return success;
	}

	public boolean isExc() {
		return !success;
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public Descriptor<?> getResultDescriptor() {
		return success ? method.getResult() : method.getExc();
	}
}
