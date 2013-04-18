package io.pdef;

import com.google.common.base.Objects;
import io.pdef.descriptors.MethodDescriptor;

import static com.google.common.base.Preconditions.checkNotNull;

public class Invocation {
	private final MethodDescriptor method;
	private final Object[] args;

	public Invocation(final MethodDescriptor method, final Object[] args) {
		this.method = checkNotNull(method);
		this.args = args.clone();
	}

	public MethodDescriptor getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args.clone();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(method.getName())
				.addValue(args)
				.toString();
	}
}
